package com.group11.hw3.chord

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import com.group11.hw3._
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext}
import scala.util.control.Breaks._

object ChordClassicNode {

  def props(): Props = Props(new ChordClassicNode())
  val numberOfShards=100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(id, payload) => (id.toString, payload)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case EntityEnvelope(id, _)       => (id % numberOfShards).toString
    case ShardRegion.StartEntity(id) =>
      // StartEntity is used by remembering entities feature
      (id.toLong % numberOfShards).toString
  }

}

class ChordClassicNode() extends Actor with ActorLogging{
  implicit val ec: ExecutionContext = context.dispatcher
  val nodeConf: Config = context.system.settings.config
  val ringSize: BigInt = BigInt(2).pow(nodeConf.getInt("networkConstants.M"))

  val nodeHash : BigInt= BigInt(self.path.name)
  var shardRegion: ActorRef= null
  var poc: BigInt = _

  var predecessorId: BigInt = nodeHash
  var successorId: BigInt = nodeHash

  var nodeData = new mutable.HashMap[BigInt,Int]()
  val numFingers: Int = nodeConf.getInt("networkConstants.M")
  var fingerTable = new Array[ClassicFinger](numFingers)

  fingerTable.indices.foreach( i =>{
    val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
    fingerTable(i) = ClassicFinger(start, nodeHash)
  } )

  /**
   * Check whether a given value lies within a range. This function also takes care of zero crossover.
   * @param leftInclude : Flag to indicate if left bound is to be included
   * @param leftValue : Left bound of the interval
   * @param rightValue : Right bound of the interval
   * @param rightInclude : Flag to indicate if left bound is to be included
   * @param valueToCheck : Value which is checked for its presence between the left and right bounds of interval
   * @return Boolean indicating presence or absence
   */
  def checkRange( leftInclude: Boolean,
                  leftValue: BigInt,
                  rightValue: BigInt,
                  rightInclude: Boolean,
                  valueToCheck: BigInt): Boolean = {
    if (leftValue == rightValue) {
      true
    }
    else if (leftValue < rightValue) {
      (valueToCheck == leftValue && leftInclude) ||
        (valueToCheck == rightValue && rightInclude) ||
        (valueToCheck > leftValue && valueToCheck < rightValue)
    }
    else {
      (valueToCheck == leftValue && leftInclude) ||
        (valueToCheck == rightValue && rightInclude) ||
        (valueToCheck > leftValue || valueToCheck < rightValue)
    }

  }

  /**
   * Update finger tables of all nodes in the network ofter a new node is added.
   */
  def updateFingerTablesOfOthers(): Unit = {
    val M=nodeConf.getInt("networkConstants.M")
    for (i <- 0 until M ) {
      val p = (nodeHash - BigInt(2).pow(i) + BigInt(2).pow(M) + 1) % BigInt(2).pow(M)
      shardRegion ! EntityEnvelope(successorId , CUpdateFingerTable(nodeHash,i,p))
    }
  }

  /**
   * Print finger table
   * @return fingerTableStatus : Returns a string representing current state of the finger table.
   */
  def getFingerTableStatus(): String = {
    var fingerTableStatus: String = "[ "
    for (finger <- fingerTable) {
      fingerTableStatus = fingerTableStatus+"( "+finger.start.toString+" : "+finger.nodeId.toString+" ), "
    }
    fingerTableStatus = fingerTableStatus + "]"
    fingerTableStatus
  }

  /**
   * Function to search the finger table to find a node which is the closest predecessor of given key
   * @param key : key hash whose predecessor is needed.
   * @return resPredId  : Hash ID of predecessor found.
   */
  def findClosestPredInFT(key:BigInt):BigInt = {
    var resPredId: BigInt = nodeHash

    /*
      Assuming the key is not equal to my hash
      Go through the finger table and find the closest finger preceding the key
      Check if key is not found between any two fingers.
      If not, it is beyond the last finger, hence the last finger is the closest predecessor.
     */

    var found = false
    breakable {
      for (i <- 0 until numFingers - 1) {
        val curFingernode = fingerTable(i).nodeId
        val nextFingerNode = fingerTable(i + 1).nodeId
        // If key is between current finger node and next finger node, then return current finger node
        if (checkRange(false, curFingernode, nextFingerNode, true, key)) {
          resPredId = fingerTable(i).nodeId
          found = true
          break
        }
      }
    }

    // If key is beyond the last finger node, return the last finger node.
    if (!found) {
      resPredId = fingerTable(numFingers-1).nodeId
    }

    // If no closest node found in the finger table, return self
    resPredId
  }

  log.info("Classic actor created")

  override def receive: Receive = {

    case CJoinNetwork(chordShardRegion,peerID) => {
      /*
       We assume network has at least one node and so, networkRef is not null
       */

      log.info("Join network called for node "+nodeHash.toString)
      this.shardRegion=chordShardRegion
      if(peerID != nodeHash) {

        this.poc = peerID
        implicit val timeout: Timeout = Timeout(5.seconds)
        val future = shardRegion ? EntityEnvelope(poc , CGetNodeNeighbors(nodeHash))
        val nodeNbrResp = Await.result(future, timeout.duration).asInstanceOf[CGetNodeNeighborsResponse]

        fingerTable(0).nodeId = nodeNbrResp.succId
        successorId = nodeNbrResp.succId
        predecessorId = nodeNbrResp.predId

        shardRegion ! EntityEnvelope(successorId, CSetNodePredecessor(nodeHash))
        shardRegion ! EntityEnvelope(predecessorId, CSetNodeSuccessor(nodeHash))

        for (i <- 1 until numFingers) {
          val lastSucc = fingerTable(i - 1).nodeId
          val curStart = fingerTable(i).start

          if (checkRange(false, nodeHash, lastSucc, true, curStart)) {
            log.debug("finger carry over.")
            fingerTable(i).nodeId = fingerTable(i - 1).nodeId
          }
          else {
            log.debug("finding new successor for finger. ")
            implicit val timeout: Timeout = Timeout(5.seconds)
            val future = shardRegion ? EntityEnvelope(poc , CGetNodeNeighbors(curStart))
            val nodeNbrResp = Await.result(future, timeout.duration).asInstanceOf[CGetNodeNeighborsResponse]
            fingerTable(i).nodeId = nodeNbrResp.succId

          }
        }
        log.debug(getFingerTableStatus())
        updateFingerTablesOfOthers()
      }
      log.info("{} added to chord network", nodeHash)
      sender() ! CJoinStatus("Complete!")
    }

    case CGetNodeNeighbors(key) => {
      var nodesuccId: BigInt = null
      var nodepredId: BigInt = null
      if (checkRange(false, nodeHash, successorId, true, key)) {
        nodesuccId = successorId
        nodepredId = nodeHash
        sender ! CGetNodeNeighborsResponse(nodesuccId,nodepredId)
      }
      else if (checkRange(false, predecessorId, nodeHash, true, key)) {
        nodesuccId = nodeHash
        nodepredId = predecessorId
        sender ! CGetNodeNeighborsResponse(nodesuccId,nodepredId)
      }
      else {
        val next_pocId = findClosestPredInFT(key)
        implicit val timeout: Timeout = Timeout(5.seconds)
        (shardRegion ? EntityEnvelope(next_pocId , CGetNodeNeighbors(key))).pipeTo(sender())
      }
    }

    case CSetNodeSuccessor(id) => {
      successorId = id
      fingerTable(0).nodeId = id
    }

    case CSetNodePredecessor(id) => {
      predecessorId = id
    }

    case CUpdateFingerTable(id,i,pos) => {
      if (id != nodeHash) {
        if (checkRange(leftInclude = false, nodeHash, fingerTable(0).nodeId, rightInclude = true, pos)) {
          if (checkRange(leftInclude = false, nodeHash, fingerTable(i).nodeId, rightInclude = false, id)) {
            fingerTable(i).nodeId = id
            shardRegion ! EntityEnvelope(predecessorId , CUpdateFingerTable( id,i,nodeHash))
          }
        } else {
          val nextPredId = findClosestPredInFT(pos)
          shardRegion ! EntityEnvelope(nextPredId , CUpdateFingerTable( id,i,pos))
        }
      }
    }

    case CGetFingerTableStatus() => {
      sender ! CFingerTableStatusResponse(getFingerTableStatus())
    }

    case CReadKeyValue(key: BigInt) => {
      log.debug("Received read request at node : "+nodeHash+" for key : "+key)
      /*
       If key lies between my predecessor and me, I should store the key value pair.
       Else, if I am the closest predecessor to key, forward the request to my successor.
       Else, find the closest predecessor to key and forward the request
       */
      if (checkRange(leftInclude = false, predecessorId, nodeHash, rightInclude = true, key)) {
        if (nodeData.contains(key)) {
          val value = nodeData(key).toString
          log.info("READ REQUEST key : "+key+" satisfied by node : "+nodeHash+" value : "+value)
          sender ! CReadResponse(value)
        } else {
          log.error("Key : "+key+" not found at node : "+nodeHash)
          sender ! CReadResponse("Key :"+key+" not found at node : "+nodeHash)
        }
      }
      else if (checkRange(leftInclude = false, nodeHash, fingerTable(0).nodeId, rightInclude = true, key)) {
        implicit val timeout: Timeout = Timeout(10.seconds)
        (shardRegion ? EntityEnvelope(fingerTable(0).nodeId, CReadKeyValue(key))).pipeTo(sender())
      }
      else {
        implicit val timeout: Timeout = Timeout(10.seconds)
        val target = findClosestPredInFT(key)
        (shardRegion ? EntityEnvelope(target, CReadKeyValue(key))).pipeTo(sender())
      }
    }

    case CWriteKeyValue(key: BigInt, value: Int) => {
      log.debug("Received write request at node : "+nodeHash+" for key : "+key)
      /*
       If key lies between my predecessor and me, I should store the key value pair.
       Else, if I am the closest predecessor to key, forward the request to my successor.
       Else, find the closest predecessor to key and forward the request
       */
      if (checkRange(leftInclude = false, predecessorId, nodeHash, rightInclude = true, key)) {
        log.info("WRITE REQUEST key : "+key+" stored by node : "+nodeHash)
        if (nodeData.contains(key)) {
          nodeData.update(key, value)
        }
        else {
          nodeData.addOne(key, value)
        }
      }
      else if (checkRange(leftInclude = false, nodeHash, fingerTable(0).nodeId, rightInclude = true, key)) {
        shardRegion ! EntityEnvelope(fingerTable(0).nodeId , CWriteKeyValue(key, value))
      }
      else {
        val target = findClosestPredInFT(key)
        shardRegion ! EntityEnvelope(target , CWriteKeyValue(key, value))
      }
    }

    case _ => log.info("Chord node actor recieved a generic message.")

  }

}
