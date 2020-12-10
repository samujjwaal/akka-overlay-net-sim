package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.util.Timeout
import com.group11.hw3.chord.ChordClassicNode
import com.typesafe.config.{Config, ConfigFactory}
import scalaj.http.{HttpOptions, HttpRequest}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

/**
 * Class used to test the basic working of the akka http server.
 */
object ChordMain {
  def execute(): Unit = {

    val conf: Config = ConfigFactory.load("application.conf")
    val numNodes: Int = conf.getInt("networkConstants.numNodes")
    val netConf = conf.getConfig("networkConstants")

    /*
     Create Chord System
     */
    implicit val chordSystem = ActorSystem(netConf.getString("networkSystemName"))
    implicit val execContext:ExecutionContextExecutor=chordSystem.dispatcher
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
    val bindingFuture=Http().bindAndHandle(r, "localhost")

    Thread.sleep(100)

    /*
     Create simulation data
     */
    val data = new ListBuffer[(BigInt,Int)]()
    val dataKeys = new ListBuffer[BigInt]()
    val maxKey = netConf.getInt("maxKey")
    val totalRecords = netConf.getInt("totalRecords")
    while (data.size < totalRecords) {
      val key = scala.util.Random.nextInt(maxKey)
      val value = scala.util.Random.nextInt(1000)
      if (!dataKeys.contains(key)) {
        data.addOne((key, value))
        dataKeys.addOne(key)
//        println("key : "+key+" val : "+value)
      }
    }

    val request: HttpRequest = scalaj.http.Http("http://localhost:9000/chordRoot")

    /*
     Write Initial data to Chord
     */
    val recordsToWrite = netConf.getInt("recordsToWrite")
    var lastIndexWritten = -1
    while (lastIndexWritten < recordsToWrite-1) {
      val writeResponse = request.params(("name", data(lastIndexWritten+1)._1.toString), ("val", data(lastIndexWritten+1)._2.toString)).method("POST").option(HttpOptions.connTimeout(10000)).asString
      lastIndexWritten += 1
    }

    Thread.sleep(100)

    /*
     Start user request simulation.
     */
    val totalRequests = netConf.getInt("totalRequests")
    var numReq = 0
    while (numReq < totalRequests) {
      val requestType = scala.util.Random.nextInt(2)
      if (requestType == 0) {
        val recordToRead = scala.util.Random.nextInt(lastIndexWritten+1)
        val readResponse = request.param("name",data(recordToRead)._1.toString).option(HttpOptions.connTimeout(10000)).asString
        numReq += 1
      }
      else if (lastIndexWritten < totalRecords-1){
        val writeResponse = request.params(("name", data(lastIndexWritten+1)._1.toString), ("val", data(lastIndexWritten+1)._2.toString)).method("POST").option(HttpOptions.connTimeout(10000)).asString
        lastIndexWritten += 1
        numReq += 1
        Thread.sleep(100)
      }
    }

    Thread.sleep(1000)
    val hopsPerReq = server.hopsPerReq
    var reqPerNode = 0.toDouble
    for (node <- chordNodesId) {
      val future = (chordShardRegion ? EntityEnvelope(node, GetStats())).mapTo[Int]
      future.onComplete( {
        case Success(value) => {
          reqPerNode += value.toDouble
        }
      } )
    }
    Thread.sleep(1000)

    println("avg req per node = "+(reqPerNode/numNodes))
    println("avg hops per req = "+(hopsPerReq/totalRequests))


    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => chordSystem.terminate())

    Thread.sleep(2000)
  }

}
