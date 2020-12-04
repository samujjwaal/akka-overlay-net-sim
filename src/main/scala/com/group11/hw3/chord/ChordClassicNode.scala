package com.group11.hw3.chord

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.group11.hw3._
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.Await
import scala.util.control.Breaks._

object ChordClassicNode {

  def props(nodeHash:BigInt):Props= {
    Props(new ChordClassicNode(nodeHash:BigInt))
  }

}

class ChordClassicNode(nodeHash:BigInt) extends Actor with ActorLogging{
//  implicit val timeout: Timeout = Timeout(10.seconds)

  val nodeConf: Config = context.system.settings.config
  val ringSize: BigInt = BigInt(2).pow(nodeConf.getInt("networkConstants.M"))

  var poc: ActorRef = _
  var successor:ActorRef= self
  var predecessor:ActorRef=self

  var predecessorId: BigInt = nodeHash
  var successorId: BigInt = nodeHash

  var nodeData = new mutable.HashMap[BigInt,Int]()
  val numFingers: Int = nodeConf.getInt("networkConstants.M")
  var fingerTable = new Array[ClassicFinger](numFingers)

  fingerTable.indices.foreach(i =>{
    val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
    fingerTable(i) = ClassicFinger(start, self, nodeHash)
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
      if ((valueToCheck == leftValue && leftInclude) || (valueToCheck == rightValue && rightInclude) || (valueToCheck > leftValue && valueToCheck < rightValue)) {
        true
      } else {
        false
      }
    } else {
      if ((valueToCheck == leftValue && leftInclude) || (valueToCheck == rightValue && rightInclude) || (valueToCheck > leftValue || valueToCheck < rightValue)) {
        true
      } else {
        false
      }
    }

  }

  /**
   * Update finger tables of all nodes in the network ofter a new node is added.
   */
  def updateFingerTablesOfOthers(): Unit = {
    val M=nodeConf.getInt("networkConstants.M")
    for (i <- 0 until M ) {
      val p = (nodeHash - BigInt(2).pow(i) + BigInt(2).pow(M) + 1) % BigInt(2).pow(M)
//      var (pred,predID)=findKeyPredecessor(p)
      successor ! CUpdateFingerTable(self,nodeHash,i,p)
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
   * @return resPredRef : Ref of the predecessor found.
   *         resPredId  : Hash ID of predecessor found.
   */
  def findClosestPredInFT(key:BigInt):(ActorRef,BigInt) = {
    var resPredRef: ActorRef = self
    var resPredId: BigInt = nodeHash

    // Assuming the key is not equal to my hash

    // Go through the finger table and find the closest finger preceding the key
    // Check if key is not found between any two fingers.
    // If not, it is beyond the last finger, hence the last finger is the closest predecessor.
    var found = false
    breakable {
      for (i <- 0 until numFingers - 1) {
        val curFingernode = fingerTable(i).nodeId
        val nextFingerNode = fingerTable(i + 1).nodeId
        // If key is between current finger node and next finger node, then return current finger node
        if (checkRange(false, curFingernode, nextFingerNode, true, key)) {
          resPredRef = fingerTable(i).nodeRef
          resPredId = fingerTable(i).nodeId
          found = true
          break
        }
      }
    }

    // If key is beyond the last finger node, return the last finger node.
    if (!found) {
      resPredId = fingerTable(numFingers-1).nodeId
      resPredRef = fingerTable(numFingers-1).nodeRef
    }

    // If no closest node found in the finger table, return self
    (resPredRef,resPredId)
  }

  /**
   * Find Predecessor of the given key.
   * @param key : key hash whose predecessor is needed.
   * @return resPredRef : Ref of the predecessor found.
   *         resPredId  : Hash ID of predecessor found.
   */
  def findKeyPredecessor(key:BigInt): (ActorRef, BigInt) = {
    var resPredRef: ActorRef = self
    var resPredId: BigInt = nodeHash

    // Check my immediate neighbors for the predecessor.
    // If the key lies between my hash and my successor's hash, return myself
    if (checkRange(leftInclude = false, nodeHash, successorId, rightInclude = true, key)) {
      resPredRef = self
      resPredId = nodeHash
    }

    // If the key lies between my hash and my predecessor's hash, return my predecessor
    else if (checkRange(leftInclude = false, predecessorId, nodeHash, rightInclude = true, key)) {
      resPredRef = predecessor
      resPredId = predecessorId
    }

    // Predecessor not found in immediate neighbors.
    // Find closest predecessor in finger table and forward the request.
    else {
      val (fingerNodeRef, fingerNodeId) = findClosestPredInFT(key)
      implicit val timeout: Timeout = Timeout(10.seconds)
      val future = fingerNodeRef ? CFindKeyPredecessor(key)
      val predNode = Await.result(future, timeout.duration).asInstanceOf[CFindKeyPredResponse]
      resPredRef = predNode.predRef
      resPredId = predNode.predId
    }
    (resPredRef,resPredId)
  }

  log.info("Classic actor created")
  override def receive: Receive = {

    case CJoinNetwork(networkRef) => {
      // We assume network has at least one node and so, networkRef is not null

      println("Join network called for node "+nodeHash.toString)

      this.poc = networkRef
      implicit val timeout: Timeout = Timeout(5.seconds)
      val future = poc ? CGetNodeNeighbors(nodeHash)
      val nodeNbrResp = Await.result(future, timeout.duration).asInstanceOf[CGetNodeNeighborsResponse]

      println("--- Finding succ --- Node :" + nodeHash.toString + " succ : " + nodeNbrResp.succId.toString)
      fingerTable(0).nodeRef = nodeNbrResp.succRef
      fingerTable(0).nodeId = nodeNbrResp.succId
      successor = nodeNbrResp.succRef
      successorId = nodeNbrResp.succId
      predecessor = nodeNbrResp.predRef
      predecessorId = nodeNbrResp.predId
      successor ! CSetNodePredecessor(nodeHash, self)
      predecessor ! CSetNodeSuccessor(nodeHash, self)
      for (i <- 1 until numFingers) {

//        println("updating finger "+i+" for node "+nodeHash.toString)
        val lastSucc = fingerTable(i - 1).nodeId
        val curStart = fingerTable(i).start

        if (checkRange(false, nodeHash, lastSucc, false, curStart)) {
          fingerTable(i).nodeId = fingerTable(i - 1).nodeId
          fingerTable(i).nodeRef = fingerTable(i - 1).nodeRef
        }
        else {

          implicit val timeout: Timeout = Timeout(5.seconds)
          val future = poc ? CGetNodeNeighbors(curStart)
          val nodeNbrResp = Await.result(future, timeout.duration).asInstanceOf[CGetNodeNeighborsResponse]
          fingerTable(i).nodeRef = nodeNbrResp.succRef
          fingerTable(i).nodeId = nodeNbrResp.succId

        }
      }
      println(getFingerTableStatus())
      updateFingerTablesOfOthers()
      log.info("{} added to chord network", nodeHash)
      sender() ! CJoinStatus("Complete!")
    }

    case CGetNodeNeighbors(key) => {
      var nodesuccRef: ActorRef = null
      var nodesuccId: BigInt = null
      var nodepredRef: ActorRef = null
      var nodepredId: BigInt = null
      if (checkRange(false, nodeHash, successorId, true, key)) {
        nodesuccRef = successor
        nodesuccId = successorId
        nodepredRef = self
        nodepredId = nodeHash
      }
      else if (checkRange(false, predecessorId, nodeHash, true, key)) {
        nodesuccRef = self
        nodesuccId = nodeHash
        nodepredRef = predecessor
        nodepredId = predecessorId
      }
      else {
        val (next_pocRef, next_pocId) = findClosestPredInFT(key)
        implicit val timeout: Timeout = Timeout(5.seconds)
        val future = next_pocRef ? CGetNodeNeighbors(key)
        val nodeNbrResp = Await.result(future, timeout.duration).asInstanceOf[CGetNodeNeighborsResponse]
        nodesuccRef = nodeNbrResp.succRef
        nodesuccId = nodeNbrResp.succId
        nodepredRef = nodeNbrResp.predRef
        nodepredId = nodeNbrResp.predId
      }
      sender ! CGetNodeNeighborsResponse(nodesuccId,nodesuccRef,nodepredId,nodepredRef)
    }

    case CFindKeySuccessor(key) => {
      var keysuccRef: ActorRef = null
      var keysuccId: BigInt = null
      var keypredRef: ActorRef = null
      var keypredId: BigInt = null

      if (key == nodeHash) {
        keysuccRef = self
        keysuccId = nodeHash
        keypredId = predecessorId
        keypredRef = predecessor
      }
      else {
        val (ref, id) = findKeyPredecessor(key)
        keypredRef = ref
        keypredId = id
//        println("inside FindKeySuccessor. Got pred " + keypredId.toString)

        if (keypredRef == self) {
          // key is found between this node and it successor
          keysuccRef = successor
          keysuccId = successorId
          keypredId = nodeHash
          keypredRef = self
        }
        else {
          // key is found between node keypred and its successor. Get keypred's successor and reply with their ref
          implicit val timeout: Timeout = Timeout(10.seconds)
          val future = keypredRef ? CGetNodeSuccessor()
          val nodeSuccessorResponse = Await.result(future, timeout.duration).asInstanceOf[CGetNodeSuccResponse]
          keysuccId = nodeSuccessorResponse.nodeId
          keysuccRef = nodeSuccessorResponse.nodeRef
        }
      }
      sender ! CFindKeySuccResponse(keysuccId,keysuccRef,keypredId,keypredRef)
    }

    case CFindKeyPredecessor(key) => {
      val (predRef, predId) = findKeyPredecessor(key)
      // println("inside FindKeyPredecessor. got "+predId.toString)
      sender ! CFindKeyPredResponse(predId, predRef)
    }

    case CGetNodeSuccessor() => {
      sender ! CGetNodeSuccResponse(successorId, successor)
    }

    case CSetNodeSuccessor(id,ref) => {
      successor = ref
      successorId = id
      fingerTable(0).nodeRef = ref
      fingerTable(0).nodeId = id
    }

    case CSetNodePredecessor(id,ref) => {
      predecessor = ref
      predecessorId = id
    }

    case CUpdateFingerTable(ref,id,i,pos) => {
      if (ref != self) {
        if (checkRange(leftInclude = false, nodeHash, fingerTable(0).nodeId, rightInclude = true, pos)) {
          if (checkRange(leftInclude = false, nodeHash, fingerTable(i).nodeId, rightInclude = false, id)) {
            fingerTable(i).nodeRef = ref
            fingerTable(i).nodeId = id
            predecessor ! CUpdateFingerTable(ref, id,i,nodeHash)
          }
        } else {
          val (nextPredRef,nextPredId) = findClosestPredInFT(pos)
          nextPredRef ! CUpdateFingerTable(ref, id,i,pos)
        }
      }
    }

    case CGetFingerTableStatus() => {
      sender ! CFingerTableStatusResponse(getFingerTableStatus())
    }

    case CGetKeyValue(key: Int) => {
      println("Dummy value for " + key)

      if (checkRange(leftInclude = false, predecessor.path.name.toInt, nodeHash, rightInclude = true, key)) {
        if (nodeData.contains(key)) {
          sender ! CDataResponse(nodeData(key).toString)
        } else {
          sender ! CDataResponse("Key not found!")
        }
      } else {
        if (checkRange(leftInclude = false, nodeHash, fingerTable(0).nodeId, rightInclude = true, key)) {
          implicit val timeout: Timeout = Timeout(10.seconds)
          val future = fingerTable(0).nodeRef ? CGetValueFromNode(key)
          val keyValue = Await.result(future, timeout.duration).asInstanceOf[CDataResponse]
          sender ! CDataResponse(keyValue.message)
        } else {
          implicit val timeout: Timeout = Timeout(10.seconds)
          val (target, dummy) = findClosestPredInFT(key)
          val future = target ? CGetKeyValue(key)
          val keyValue = Await.result(future, timeout.duration).asInstanceOf[CDataResponse]
          sender ! CDataResponse(keyValue.message)
        }
      }
    }

    case CGetValueFromNode(key: Int) =>

      if (nodeData.contains(key)) {
        sender ! CDataResponse(nodeData(key).toString)
      } else {
        sender ! CDataResponse("Data not found")
      }

    case CWriteKeyValue(key: String, value: String) => {
      println("Received write request by classic chord node actor for:" + key + "," + value)
      log.info("Received write request by classic chord node actor for:" + key + "," + value)
    }

    case CFindNodeToWriteData(key: BigInt, value: Int) => {

      if (checkRange(leftInclude = false, nodeHash, fingerTable(0).nodeId, rightInclude = true, key)) {
        println("!!!Found node"+nodeHash +"for key:"+key)
        fingerTable(0).nodeRef ! CWriteDataToNode(key, value)
      } else {
        val (target, dummy) = findClosestPredInFT(key)
        println("!!!Forwarding to node"+target.path.name +"for key:"+key)
        target ! CFindNodeToWriteData(key, value)
      }
    }

    case CWriteDataToNode(key: BigInt, value: Int) => {
      log.info("Key %s  should be owned owned by node %s.".format(key, self.path.name))
      if (nodeData.contains(key)) {
        nodeData.update(key, value)
      } else {
        nodeData.addOne(key, value)
      }
    }
    case _ => log.info("Chord node actor recieved a generic message.")

  }

}
