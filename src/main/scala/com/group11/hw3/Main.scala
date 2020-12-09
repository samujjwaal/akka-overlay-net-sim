package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.util.Timeout
import com.group11.hw3.chord.ChordClassicNode
import com.typesafe.config.{Config, ConfigFactory}
import scalaj.http.{ HttpOptions, HttpRequest}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

/*
Class used to test the basic working of the akka http server.
 */
object Main {
  def main(args: Array[String]): Unit = {

    val conf: Config = ConfigFactory.load("application.conf")
    val numNodes: Int = conf.getInt("networkConstants.numNodes")
    val netConf = conf.getConfig("networkConstants")
    val userConf = conf.getConfig("userConstants")
    implicit val chordSystem = ActorSystem(netConf.getString("networkSystemName"))

    val chordNodesId = new ListBuffer[BigInt]()

//    val chordMaster = chordSystem.actorOf(ChordMaster.props())
//    implicit val timeout: Timeout = Timeout(20.seconds)
//    val future = chordMaster ? CreateNodes
//    val createNodesReply = Await.result(future, timeout.duration).asInstanceOf[CreateNodesReply]


    val chordShardRegion: ActorRef = ClusterSharding(chordSystem).start(
      typeName = "ChordNodeRegion",
      entityProps = Props[ChordClassicNode](),
      settings = ClusterShardingSettings(chordSystem),
      extractEntityId = ChordClassicNode.extractEntityId,
      extractShardId = ChordClassicNode.extractShardId
    )

    var hashID = BigInt(0)
    val peer=hashID
    //chordShardRegion ? EntityEnvelope(hashID, CJoinNetwork(chordShardRegion,peer))
    implicit val timeout = Timeout(15 seconds)
    val future = chordShardRegion ? EntityEnvelope(hashID , CJoinNetwork(chordShardRegion,peer))
    val joinStatus = Await.result(future,timeout.duration).asInstanceOf[CJoinStatus]
    //val poc = context.actorOf(ChordClassicNode.props(hashID), hashID.toString())
    chordNodesId += hashID
    //chordNodesRef.addOne(hashID,poc)

    while (chordNodesId.size < numNodes) {
      //    val hashID=ChordUtils.md5(chordNodes.size.toString)
      val hashID = BigInt(scala.util.Random.nextInt(conf.getInt("networkConstants.nodeSpace")))
      if (!(chordNodesId.contains(hashID))) {
        //val newNode = context.actorOf(ChordClassicNode.props(hashID), hashID.toString())
        implicit val timeout = Timeout(15 seconds)
        val future = chordShardRegion ? EntityEnvelope(hashID , CJoinNetwork(chordShardRegion,peer))
        val joinStatus = Await.result(future,timeout.duration).asInstanceOf[CJoinStatus]
        //log.info("Join status "+joinStatus.status+" for node "+hashID.toString)
        chordNodesId += hashID
        //chordNodesRef.addOne(hashID,newNode)
        Thread.sleep(100)
      }
    }

//    println("All nodes created...")
//    Thread.sleep(1000)
//    println("Printing all finger tables -----")
//    for (i <- chordNodesId) {
//      implicit val timeout = Timeout(15 seconds)
//      val future = chordShardRegion  ? EntityEnvelope(i,CGetFingerTableStatus())
//      val fingerStatus = Await.result(future, timeout.duration).asInstanceOf[CFingerTableStatusResponse]
//      println("Node : "+i.toString+" FT : "+fingerStatus.ft)
//    }

    //Writing initial data to nodes
    val dataList = new ArrayBuffer[Array[String]]()
    dataList.addOne(Array("1", "1998"))
    dataList.addOne(Array("7", "2000"))
    dataList.addOne(Array("14", "1996"))
    dataList.addOne(Array("20", "1920"))
    dataList.foreach(data => {
      val key = data(0)
      val value = data(1)
      val rnd = new Random
      val randomNum = 0 + rnd.nextInt((chordNodesId.size - 0) + 1)
      val randNode= chordNodesId(0)
      chordShardRegion  ! EntityEnvelope(randNode,CWriteKeyValue(BigInt(key), value.toInt))
      //initialWriteCounter.addAndGet(1)
    })

    Thread.sleep(2000)
    val server = new HTTPServer()
    val r=server.setupServer(chordSystem,chordShardRegion,chordNodesId.toList)
    Http().bindAndHandle(r, "localhost")

    Thread.sleep(100)

    val request: HttpRequest = scalaj.http.Http("http://localhost:9000/chordRoot")

    val writeResponse = request.params(("name", "23"), ("val", "8907")).method("POST").option(HttpOptions.connTimeout(10000)).asString
    val readResponse = request.param("name","1").option(HttpOptions.connTimeout(10000)).asString


//    val userSystem = ActorSystem(userConf.getString("userSystemName"))
//    val userMaster = userSystem.actorOf(UserMaster.props(),"user-master")
//    userMaster ! CreateUsers
//    userMaster ! StartUserRequests

//    sys ! CaptureGlobalSnapshot()

  }

}
