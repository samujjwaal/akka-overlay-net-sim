package com.group11.hw3
import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.google.gson.{GsonBuilder, JsonObject}
import com.group11.hw3.chord._
import com.typesafe.config.Config
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object ChordMaster {

  def props(): Props = Props(new ChordMaster)
}

class ChordMaster extends Actor with ActorLogging {

  val conf: Config = context.system.settings.config
  val chordNodesId = new ListBuffer[BigInt]()
  val chordNodesRef = new mutable.HashMap[BigInt,ActorRef]()
  val numNodes: Int = conf.getInt("networkConstants.numNodes")
  implicit val timeout = Timeout(15 seconds)

  def createNodes(): Unit = {
    // Add a node to chord network to act as point of contact (poc) for all other nodes to join.
    //  val hashID = ChordUtils.md5(chordNodes.size.toString)
    val hashID = BigInt(0)
    val poc = context.actorOf(ChordClassicNode.props(hashID), hashID.toString())
    chordNodesId += hashID
    chordNodesRef.addOne(hashID,poc)

    // Start creating and adding all other nodes to the network
    while (chordNodesId.size < numNodes) {
      //    val hashID=ChordUtils.md5(chordNodes.size.toString)
      val hashID = BigInt(scala.util.Random.nextInt(conf.getInt("networkConstants.nodeSpace")))
      if (!(chordNodesId.contains(hashID))) {
        val existingNode = poc
        val newNode = context.actorOf(ChordClassicNode.props(hashID), hashID.toString())
        chordNodesId += hashID
        chordNodesRef.addOne(hashID,newNode)
        val future = newNode ? CJoinNetwork(existingNode)
        val joinStatus = Await.result(future,timeout.duration).asInstanceOf[CJoinStatus]
        log.info("Join status "+joinStatus.status+" for node "+hashID.toString)
        Thread.sleep(100)
      }
    }

    println("All nodes created...")
    Thread.sleep(100)
    println("Printing all finger tables -----")
    for (i <- chordNodesId) {
      val future = chordNodesRef.get(i).head ? CGetFingerTableStatus()
      val fingerStatus = Await.result(future, timeout.duration).asInstanceOf[CFingerTableStatusResponse]
      println("Node : "+i.toString+" FT : "+fingerStatus.ft)
    }


    //Writing initial data to nodes
    val dataList = new ArrayBuffer[Array[String]]()
    dataList.addOne(Array("1", "1998"))
    dataList.addOne(Array("7", "2000"))
    dataList.addOne(Array("4", "1996"))
    dataList.addOne(Array("5", "1920"))
      dataList.foreach(data => {
        val key = data(0)
        val value = data(1)
        val rnd = new Random
        val randomNum = 0 + rnd.nextInt((chordNodesId.size - 0) + 1)
        val randNode= chordNodesRef.get(0)
        randNode.head ! CFindNodeToWriteData(key.toInt, value.toInt)
        //initialWriteCounter.addAndGet(1)
      })

      // Wait for 1 second to ensure all the data is written to the system
      Thread.sleep(1000)

      //log.info("Number of key value pairs written initially to the chord system during setup : %s".format(initialWriteCounter.get))

  }

  override def receive: Receive = {

    case CreateNodes =>
      {
        createNodes()
        sender ! CreateNodesReply(chordNodesRef)
      }

    case CCaptureGlobalSnapshot() =>
      println("**Received request for snapshot")
      val gson = new GsonBuilder().setPrettyPrinting().create()
      val nodesSnapshot = new JsonObject()
      var nodeSnap = new JsonObject()
      log.info("**Servicing snapshot request")

      for (node <- chordNodesId) {
        val nodeRef = chordNodesRef.get(node).head
        val future = nodeRef ? CGetNodeSnapshot()
        try {
          val response = Await.result(future, timeout.duration).asInstanceOf[GetNodeSnapshotResponse]
          val nodeSnap = response.snap
          nodesSnapshot.add("Node" + nodeSnap.get("Node"), nodeSnap)
          println(nodesSnapshot)
          val path2 = "/output/%s.json".format("/Snapshot/Nodes")
          FileUtils.write(new File(path2), gson.toJson(nodesSnapshot), "UTF-8")
        }
        catch {
          case exp: Exception =>
            log.error("Failed to get snapshot form node")
        }

      }

    case _ => log.info("Received a generic message.")

  }
}

