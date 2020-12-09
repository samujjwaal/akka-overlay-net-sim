package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.util.Timeout
import com.group11.hw3.chord.ChordClassicNode
import com.typesafe.config.{Config, ConfigFactory}
import scalaj.http.{ HttpOptions, HttpRequest}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Class used to test the basic working of the akka http server.
 */
object Main {
  def main(args: Array[String]): Unit = {

    val conf: Config = ConfigFactory.load("application.conf")
    val numNodes: Int = conf.getInt("networkConstants.numNodes")
    val netConf = conf.getConfig("networkConstants")
    val userConf = conf.getConfig("userConstants")

    /*
     Create Chord System
     */
    implicit val chordSystem = ActorSystem(netConf.getString("networkSystemName"))
    val chordNodesId = new ListBuffer[BigInt]()
    val chordShardRegion: ActorRef = ClusterSharding(chordSystem).start(
      typeName = "ChordNodeRegion",
      entityProps = Props[ChordClassicNode](),
      settings = ClusterShardingSettings(chordSystem),
      extractEntityId = ChordClassicNode.extractEntityId,
      extractShardId = ChordClassicNode.extractShardId
    )

    /*
     Add first node to Chord network
     */
    var hashID = BigInt(0)
    val peer=hashID
    implicit val timeout = Timeout(15 seconds)
    val future = chordShardRegion ? EntityEnvelope(hashID , CJoinNetwork(chordShardRegion,peer))
    val joinStatus = Await.result(future,timeout.duration).asInstanceOf[CJoinStatus]
    chordNodesId += hashID

    /*
     Add remaining nodes to Chord network
     */
    while (chordNodesId.size < numNodes) {
      val hashID = BigInt(scala.util.Random.nextInt(conf.getInt("networkConstants.nodeSpace")))
      if (!(chordNodesId.contains(hashID))) {
        implicit val timeout = Timeout(15 seconds)
        val future = chordShardRegion ? EntityEnvelope(hashID , CJoinNetwork(chordShardRegion,peer))
        val joinStatus = Await.result(future,timeout.duration).asInstanceOf[CJoinStatus]
        chordNodesId += hashID
        Thread.sleep(100)
      }
    }

    // Print finger tables of all nodes
//    println("All nodes created...")
//    Thread.sleep(1000)
//    println("Printing all finger tables -----")
//    for (i <- chordNodesId) {
//      implicit val timeout = Timeout(15 seconds)
//      val future = chordShardRegion  ? EntityEnvelope(i,CGetFingerTableStatus())
//      val fingerStatus = Await.result(future, timeout.duration).asInstanceOf[CFingerTableStatusResponse]
//      println("Node : "+i.toString+" FT : "+fingerStatus.ft)
//    }

    /*
     Start the HTTP server
     */
    Thread.sleep(1000)
    val server = new HTTPServer()
    val r=server.setupServer(chordSystem,chordShardRegion,chordNodesId.toList)
    Http().bindAndHandle(r, "localhost")

    Thread.sleep(100)

    /*
     Create simulation data
     */
    val data = new ListBuffer[(BigInt,Int)]()
    val maxKey = netConf.getInt("maxKey")
    val totalRecords = netConf.getInt("totalRecords")
    while (data.size < totalRecords) {
      val key = scala.util.Random.nextInt(maxKey)
      val value = scala.util.Random.nextInt(1000)
      if (!data.contains(key)) {
        data.addOne((key, value))
      }
    }

    val request: HttpRequest = scalaj.http.Http("http://localhost:9000/chordRoot")

    /*
     Write Initial data to Chord
     */
    val recordsToWrite = netConf.getInt("recordsToWrite")
    var indexWrittenToChord = -1
    for (i <- 0 until recordsToWrite) {
      val writeResponse = request.params(("name", data(i)._1.toString), ("val", data(i)._2.toString)).method("POST").option(HttpOptions.connTimeout(10000)).asString
      indexWrittenToChord = i
    }

    Thread.sleep(100)

    /*
     Start user request simulation.
     */
    val totalRequests = netConf.getInt("totalRequests")

    for (j <- 0 until totalRequests) {
      val requestType = scala.util.Random.nextInt(2)
      if (requestType == 0) {
        val recordToRead = scala.util.Random.nextInt(indexWrittenToChord+1)
        val readResponse = request.param("name",data(recordToRead)._1.toString).option(HttpOptions.connTimeout(10000)).asString
      }
      else if (indexWrittenToChord < totalRecords-1){
        val writeResponse = request.params(("name", data(indexWrittenToChord+1)._1.toString), ("val", data(indexWrittenToChord+1)._2.toString)).method("POST").option(HttpOptions.connTimeout(10000)).asString
        indexWrittenToChord +=1
        Thread.sleep(100)
      }
    }

  }

}
