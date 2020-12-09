package com.group11.can

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import com.group11.can.CanMessageTypes._
import com.typesafe.config.Config

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps



object CanNode {
  def props(id:BigInt):Props= {
    Props(new CanNode())
  }
  val numberOfShards=100
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(id, payload) => (id.toString, payload)
    //case msg @ Get(id)               => (id.toString, msg)
  }


  val extractShardId: ShardRegion.ExtractShardId = {
    case EntityEnvelope(id, _)       => (id % numberOfShards).toString
    //case Get(id)                     => (id % numberOfShards).toString
    case ShardRegion.StartEntity(id) =>
      // StartEntity is used by remembering entities feature
      (id.toLong % numberOfShards).toString
  }
}

class CanNode() extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher

  val nodeConf: Config = context.system.settings.config.getConfig("CANnetworkConstants")
  val xMax: Double = nodeConf.getDouble("xMax")
  val yMax: Double = nodeConf.getDouble("yMax")
  var myCoord: Coordinate = null
  val myNeighbors = new mutable.HashMap[BigInt,Neighbor]()
  val myData = new mutable.HashMap[(Double,Double),Int]()
  val myId=BigInt(self.path.name)
  var shardRegion: ActorRef = null

  var requestsSatisfied = 0
  def nbrsAsString(): String = {
    var str = ""
    for (n <- myNeighbors) {
      str += n._1.toString() + n._2.getAsString()
    }
    str
  }

  def splitMyZone(newNode: BigInt) = {

    var incomingUpperX=Double.MinValue
    var incomingLowerX=Double.MinValue
    var incomingUpperY=Double.MinValue
    var incomingLowerY=Double.MinValue
    if(myCoord.canSplitVertically)
    {
      log.info("Splitting vertically for "+newNode)
      val oldUpperX=myCoord.upperX
      myCoord.splitVertically()
      incomingLowerX=myCoord.upperX
      incomingLowerY=myCoord.lowerY
      incomingUpperX=oldUpperX
      incomingUpperY=myCoord.upperY
    }
    else
    {
      log.info("Splitting horizontally for "+newNode)
      val oldUpperY=myCoord.upperY
      myCoord.splitHorizontally()

      incomingLowerX=myCoord.lowerX
      incomingLowerY=myCoord.upperY
      incomingUpperX=myCoord.upperX
      incomingUpperY=oldUpperY
    }

    addNbr(newNode,incomingLowerX,incomingLowerY,incomingUpperX,incomingUpperY)
    shardRegion ! EntityEnvelope(newNode,AddNeighbor( myCoord.lowerX:Double, myCoord.lowerY:Double,
                                                      myCoord.upperX:Double, myCoord.upperY:Double,
                                                      myId))
    val incomingCoord= new Coordinate(incomingLowerX,incomingLowerY,incomingUpperX,incomingUpperY)

    var nbrToRemove = new ListBuffer[BigInt]()
    for( n <- myNeighbors)
    {
      val nbrCoord = n._2.nodeCoord
      if((incomingCoord.isAdjacentX(nbrCoord) && incomingCoord.isSubsetY(nbrCoord)) ||
          (incomingCoord.isAdjacentY(nbrCoord) && incomingCoord.isSubsetX(nbrCoord)))
      {
        shardRegion ! EntityEnvelope(n._1,AddNeighbor(incomingLowerX,incomingLowerY,incomingUpperX,incomingUpperY,newNode))

        shardRegion ! EntityEnvelope(newNode,AddNeighbor(nbrCoord.lowerX,nbrCoord.lowerY,nbrCoord.upperX,nbrCoord.upperY,n._1))
      }

      if((myCoord.isAdjacentX(nbrCoord) && myCoord.isSubsetY(nbrCoord)) ||
          (myCoord.isAdjacentY(nbrCoord) && myCoord.isSubsetX(nbrCoord))) {

        shardRegion ! EntityEnvelope(n._1,UpdateNeighbor( myId: BigInt,
                                                          myCoord.lowerX:Double, myCoord.lowerY:Double,
                                                          myCoord.upperX:Double, myCoord.upperY:Double))
      }
      else {

        shardRegion ! EntityEnvelope(n._1,RemoveNeighbor(myId))
        nbrToRemove.addOne(n._1)
      }
    }
    myNeighbors --= nbrToRemove
    (incomingLowerX,incomingLowerY,incomingUpperX,incomingUpperY)

  }

  def addNbr(nbrID: BigInt,lx:Double,ly:Double,ux:Double,uy:Double): Unit = {
    val nbrCoord = new Coordinate(lx:Double,ly:Double,ux:Double,uy:Double)
    val nbr = new Neighbor(nbrCoord,nbrID)
    myNeighbors(nbrID) = nbr
  }

  def findClosestNeighbor(p_x: Double, p_y: Double): BigInt = {
    var closestNbr: BigInt = null
    var dist = Double.MaxValue
    for (nbr <- myNeighbors) {
      val nbrDist = nbr._2.nodeCoord.dist(p_x,p_y)
      if (nbrDist < dist) {
        dist = nbrDist
        closestNbr = nbr._1
      }
    }
    closestNbr
  }

  override def receive: Receive = {

    case JoinCan(shardRegionRef: ActorRef, existingNode:BigInt) => {
//      println("Join called for node:"+ myId)
      log.info("Join called for node:"+ myId)
      shardRegion = shardRegionRef
      if (existingNode == myId) {
        myCoord = new Coordinate(0,0,xMax,yMax)
      }
      else {
        val p_x = scala.util.Random.nextDouble() * xMax
        val p_y = scala.util.Random.nextDouble() * yMax
//        log.info("Join called for node:"+ myId+" x: "+p_x+" y: "+p_y)
        implicit val timeout = Timeout(10 seconds)
        val future= shardRegionRef ? EntityEnvelope(existingNode,RouteNewNode(p_x, p_y, myId))
        val coords = Await.result(future,timeout.duration).asInstanceOf[RouteResponse]
        myCoord = new Coordinate(coords.lx,coords.ly,coords.ux,coords.uy)
//        peer ! RouteNewNode(p_x, p_y, self)
      }
      sender() ! JoinDone("Done")
    }

    case RouteNewNode(p_x, p_y, newNode) => {
//      println("Route request received at "+myId)
      log.info("Route request received at "+myId+" for "+newNode)
      if (myCoord.hasPoint(p_x,p_y)) {
        log.info("Point lies in my zone, splitting my zone. ")
        val (lx,ly,ux,uy) = splitMyZone(newNode)
        sender() ! RouteResponse(lx,ly,ux,uy)
      }
      else {
        log.info("Not in my zone. Finding my closest neighbor to forward request. my coord: "+myCoord.getAsString())
        val closestNeighbor = findClosestNeighbor(p_x,p_y)
        implicit val timeout = Timeout(10 seconds)
        (shardRegion ? EntityEnvelope(closestNeighbor,RouteNewNode(p_x, p_y, newNode))).pipeTo(sender())
      }
    }

    case GetNodeId() => {
      sender() ! myId
    }

    case GetNeighbors() => {
      sender() ! myNeighbors
    }

    case GetCoord() => {
      sender() ! myCoord
    }

    case SetCoord(l_X: Double, l_Y: Double, u_X:Double, u_Y:Double) => {

      myCoord.setCoord(l_X, l_Y, u_X, u_Y)

    }

    case AddNeighbor(lx:Double,ly:Double,ux:Double,uy:Double, nbrID: BigInt) => {
      addNbr(nbrID,lx,ly,ux,uy)
    }

    case RemoveNeighbor(nbrID: BigInt) => {
        myNeighbors.remove(nbrID)
    }

    case UpdateNeighbor(nbrID: BigInt,lx:Double,ly:Double,ux:Double,uy:Double) => {
      myNeighbors(nbrID).nodeCoord.setCoord(lx:Double,ly:Double,ux:Double,uy:Double)
    }

    case WriteData(key:(Double,Double), value:Int, hops:Int) => {
      log.info("WriteRequest at node "+myId+" for key "+key)
      if (myCoord.hasPoint(key._1,key._2)) {
        log.info("Point lies in "+myId+" write "+key+" to myData")
        myData.addOne(key,value)
        requestsSatisfied += 1
        sender() ! hops
      }
      else {
        val closestNeighbor = findClosestNeighbor(key._1,key._2)
        val total_hops = hops + 1
        implicit val timeout = Timeout(10 seconds)
        (shardRegion ? EntityEnvelope(closestNeighbor,WriteData(key,value,total_hops))).pipeTo(sender())
      }
    }

    case ReadData(key:(Double,Double), hops:Int) => {
      log.info("ReadRequest at node "+myId+" for key "+key)
      if (myCoord.hasPoint(key._1,key._2)) {
        log.info("Point lies in "+myId+", get from myData")
        if (myData.contains(key)) {
          log.info("key : " + key + " value : " + myData(key))
          requestsSatisfied += 1
          sender() ! hops
        }
        else {
          log.info("Failed to find key in myData. Node : "+myId+" key : "+key)
          sender() ! hops
        }
      }
      else {
        val closestNeighbor = findClosestNeighbor(key._1,key._2)
        val total_hops = hops + 1
        implicit val timeout = Timeout(10 seconds)
        (shardRegion ? EntityEnvelope(closestNeighbor,ReadData(key,total_hops))).pipeTo(sender())
      }
    }

    case GetStats() => {
      sender() ! requestsSatisfied
    }

    case PrintNeighbors => {
//      println("Node ID:"+myId+" my neighbors are:"+myNeighbors.size)
      log.info("Node ID:"+myId+" my neighbors are:"+myNeighbors.size)
      for( n <- myNeighbors)
        {
//          println(myId+"'s neighbor:"+n._2.getAsString())
          log.info(myId+"'s neighbor:"+n._2.getAsString())
        }
    }

  }
}
