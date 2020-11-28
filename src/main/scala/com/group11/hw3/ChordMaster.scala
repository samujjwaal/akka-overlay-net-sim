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
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object ChordMaster {

  def props(): Props = Props(new ChordMaster)
}

class ChordMaster extends Actor with ActorLogging {

  val conf: Config = context.system.settings.config
  val chordNodesId = new ListBuffer[BigInt]()
  val chordNodesRef = new mutable.HashMap[BigInt,ActorRef]()
  val numNodes: Int = conf.getInt("networkConstants.numNodes")
  implicit val timeout = Timeout(10 seconds)

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
        newNode ! CJoinNetwork(existingNode)
        Thread.sleep(100)
      }
    }
  }

  override def receive: Receive = {

    case CreateNodes =>
      {
        createNodes()
        println("**"+sender.path)
        sender ! CreateNodesReply(chordNodesRef)
      }

    case CaptureGlobalSnapshot() =>
      println("**Received request for snapshot")
      val gson = new GsonBuilder().setPrettyPrinting().create()
      val nodesSnapshot = new JsonObject()
      var nodeSnap = new JsonObject()
      log.info("**Servicing snapshot request")

      for (node <- chordNodesId) {
        val nodeRef = chordNodesRef.get(node).head
        val future = nodeRef ? ChordClassicNode.GetNodeSnapshot()
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

