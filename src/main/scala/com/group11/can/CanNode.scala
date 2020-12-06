package com.group11.can

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.group11.can.CanMessageTypes._
import com.typesafe.config.Config

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

object CanNode {
  def props(id:BigInt):Props= {
    Props(new CanNode(id:BigInt))
  }

}

class CanNode(myId:BigInt) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher

  val nodeConf: Config = context.system.settings.config.getConfig("CANnetworkConstants")
  val xMax: Double = nodeConf.getDouble("xMax")
  val yMax: Double = nodeConf.getDouble("yMax")
  var myCoord: Coordinate = _
  val myNeighbors = new ListBuffer[Neighbor]()

  def splitMyZone(newNode: ActorRef): Unit = {

    var incomingUpperX=Double.MinValue
    var incomingLowerX=Double.MinValue
    var incomingUpperY=Double.MinValue
    var incomingLowerY=Double.MinValue
    if(myCoord.canSplitVertically)
    {
      log.info("Splitting vertically.")
      val oldUpperX=myCoord.upperX
      myCoord.splitVertically()

      newNode ! SetCoord(myCoord.upperX, myCoord.lowerY, oldUpperX, myCoord.upperY)

      incomingLowerX=myCoord.upperX
      incomingLowerY=myCoord.lowerY
      incomingUpperX=oldUpperX
      incomingUpperY=myCoord.upperY
    }
    else
    {
      log.info("Splitting horizontally.")
      val oldUpperY=myCoord.upperY
      myCoord.splitHorizontally()

      newNode ! SetCoord(myCoord.lowerX, myCoord.upperY, myCoord.upperX, oldUpperY)

      incomingLowerX=myCoord.lowerX
      incomingLowerY=myCoord.upperY
      incomingUpperX=myCoord.upperX
      incomingUpperY=oldUpperY
    }

    val incomingCoord= new Coordinate(incomingLowerX,incomingLowerY,incomingUpperX,incomingUpperY)


    val incomingAsNeighbor= new Neighbor(newNode,incomingCoord,BigInt(newNode.path.name.toInt))
    myNeighbors.addOne(incomingAsNeighbor)


    val selfAsNeighbor= new Neighbor(self, myCoord, myId)
    newNode ! AddNeighbor(selfAsNeighbor)
    var nbrToRemove = new ListBuffer[Neighbor]()
    for( n <- myNeighbors)
    {
      if((incomingCoord.isAdjacentX(n.nodeCoord) && incomingCoord.isSubsetY(n.nodeCoord)) ||
          (incomingCoord.isAdjacentY(n.nodeCoord) && incomingCoord.isSubsetX(n.nodeCoord)))
      {
         n.nodeRef ! AddNeighbor(incomingAsNeighbor)
         newNode ! AddNeighbor(n)
      }

      /* TODO
      If we are still neighbors, ask this neighbor to update my coordinate.
      Otherwise remove my from its neighbors and remove this from my neighbors.
      DONE..
       */
      if((myCoord.isAdjacentX(n.nodeCoord) && myCoord.isSubsetY(n.nodeCoord)) ||
          (myCoord.isAdjacentY(n.nodeCoord) && myCoord.isSubsetX(n.nodeCoord))) {
          n.nodeRef ! UpdateNeighbor(selfAsNeighbor)
      }
      else {
        n.nodeRef ! RemoveNeighbor(selfAsNeighbor)
      /* TODO
      Instead of deleting by index, we can use a seq to collect all nbrs to be deleted and remove them together
       */
        nbrToRemove.addOne(n)
      }

    }
    myNeighbors --= nbrToRemove


  }

  def findClosestNeighbor(p_x: Double, p_y: Double): Neighbor = {
    var closestNbr: Neighbor = null
    var dist = Double.MaxValue
    for (nbr <- myNeighbors) {
      val nbrDist = nbr.nodeCoord.dist(p_x,p_y)
      if (nbrDist < dist) {
        dist = nbrDist
        closestNbr = nbr
      }
    }
    closestNbr
  }

  override def receive: Receive = {

    case JoinCan(peer: ActorRef) => {
      log.info("Join called for node:"+ peer.path.name)
      if (peer == self) {
        myCoord = new Coordinate(xMax,yMax,0,0)
      }
      else {
        val p_x = scala.util.Random.nextDouble() * xMax
        val p_y = scala.util.Random.nextDouble() * yMax
        peer ! RouteNewNode(p_x, p_y, self)
      }
    }

    case RouteNewNode(p_x, p_y, newNode) => {
      if (myCoord.hasPoint(p_x,p_y)) {
        log.info("Point lies in my zone, splitting my zone.")
        splitMyZone(newNode)
      }
      else {
        log.info("Request forwarded to my closest neighbor.")
        val closestNeighbor = findClosestNeighbor(p_x,p_y)
        closestNeighbor.nodeRef ! RouteNewNode(p_x, p_y, newNode)
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

    case AddNeighbor(newNeighbor : Neighbor) => {
        myNeighbors.addOne(newNeighbor)
    }

    case RemoveNeighbor(neighborToRemove : Neighbor) => {
        myNeighbors.remove(myNeighbors.indexOf(neighborToRemove))
    }
    case UpdateNeighbor(neighborToUpdate : Neighbor) =>{

        for(n <- myNeighbors)
          {
            if(n.nodeId == neighborToUpdate.nodeId)
              {
                myNeighbors.update(myNeighbors.indexOf(n),neighborToUpdate)
              }
          }
    }

  }
}
