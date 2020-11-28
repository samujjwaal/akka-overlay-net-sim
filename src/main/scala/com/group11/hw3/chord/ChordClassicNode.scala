package com.group11.hw3.chord
import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.group11.hw3.{CDataResponse, CFindKeyPredResponse, CFindKeyPredecessor, CFindKeySuccResponse, CFindKeySuccessor, CFingerTableStatusResponse, CGetFingerTableStatus, CGetKeyValue, CGetNodeSuccResponse, CGetNodeSuccessor, CJoinNetwork, CJoinStatus, CSetNodePredecessor, CSetNodeSuccessor, CUpdateFingerTable, CWriteKeyValue}

import scala.collection.mutable
import scala.concurrent.Await

object ChordClassicNode {
  case class JoinNetwork(existingNode: ActorRef)
  case class GetNodeSnapshot()

  def props(nodeHash:BigInt):Props= {
    Props(new ChordClassicNode(nodeHash:BigInt))
  }
}

class ChordClassicNode(nodeHash:BigInt) extends Actor with ActorLogging{
  import ChordClassicNode._
  //implicit val timeout: Timeout = Timeout(Constants.defaultTimeout)

  val nodeConf=context.system.settings.config
  val ringSize= BigInt(2).pow(nodeConf.getInt("networkConstants.M"))

  var successor:ActorRef= self
  var predecessor:ActorRef=self

  var predecessorId: BigInt = nodeHash
  var successorId: BigInt = nodeHash

  var nodeData = new mutable.HashMap[BigInt,Int]()
  var fingerTable = new Array[ClassicFinger](nodeConf.getInt("networkConstants.M"))

  fingerTable.indices.foreach(i =>{
    val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
    fingerTable(i) = ClassicFinger(start, self, nodeHash)
  } )

  //Function to check whether a given value lies within a range. This function also takes care of zero crossover.
  def checkRange(leftInclude: Boolean, leftValue: BigInt, rightValue: BigInt, rightInclude: Boolean, valueToCheck: BigInt): Boolean =
  {
    if (leftValue == rightValue) {
      true
    }
    else if (leftValue < rightValue) {
      if (valueToCheck == leftValue && leftInclude || valueToCheck == rightValue && rightInclude || (valueToCheck > leftValue && valueToCheck < rightValue)) {
        true
      } else {
        false
      }
    } else {
      if (valueToCheck == leftValue && leftInclude || valueToCheck == rightValue && rightInclude || (valueToCheck > leftValue || valueToCheck < rightValue)) {
        true
      } else {
        false
      }
    }

  }

  def updateFingerTablesOfOthers(): Unit = {
    val M=nodeConf.getInt("networkConstants.M")
    for (i <- 0 until M ) {
      var p = (nodeHash - BigInt(2).pow(i) + BigInt(2).pow(M) + 1) % BigInt(2).pow(M)
      var (pred,predID)=findKeyPredecessor(p)
      pred ! CUpdateFingerTable(self,nodeHash,i,p)
    }
  }

  //Print finger table
  def getFingerTableStatus(): String = {
    var fingerTableStatus = "[ "
    for (finger <- fingerTable) {
      fingerTableStatus = fingerTableStatus+"( "+finger.start.toString+" : "+finger.nodeId.toString+" ), "
    }
    fingerTableStatus = fingerTableStatus + "]"
    fingerTableStatus
  }

  //Function to print the closest predecessor from finger table
  def findClosestPredInFT(key:BigInt):(ActorRef,BigInt) =
  {
    var resPredRef: ActorRef = self
    var resPredId: BigInt = nodeHash
    // If my hash is equal to the key, return my predecessor
    if (key == nodeHash) {
      resPredRef = self
      resPredId = nodeHash
    }
    else {
      // Go through the finger table and find closest finger pointing to a node preceding the key
      for (i <- fingerTable.indices) {
        var index = fingerTable.size - i - 1
        // check if we can form a seq nodeHash > finger node > key
        // if yes, then return the node pointed by this finger
        var currKey = key
        var fingernode = fingerTable(index).nodeId
        // Return the finger node if it matches the key
        if (fingernode == currKey) {
          resPredRef = fingerTable(index).nodeRef
          resPredId = fingerTable(index).nodeId
        }
        else {

          if (checkRange(false,nodeHash,currKey,false,fingernode)) {
            resPredRef = fingerTable(index).nodeRef
            resPredId = fingerTable(index).nodeId
          }
        }
      }
    }
    // If no closest node found in the finger table, return self
    (resPredRef,resPredId)
  }

  def findKeyPredecessor(key:BigInt): (ActorRef, BigInt) = {
    var resPredRef: ActorRef = self
    var resPredId: BigInt = nodeHash
    // If my hash is the same as key, return my predecessor
    if (key == nodeHash) {
      resPredRef = predecessor
      resPredId = predecessorId
    }
    // If key == my successor, return self
    if (key == successorId) {
      resPredRef = self
      resPredId = nodeHash
    }

    // Check if the key lies between my hash and my successor's hash
    var succId = successorId
    var currentKey = key

    // If key is in the interval, return self as the predecessor
    if (checkRange(false,nodeHash,succId,false,currentKey)) {
      resPredRef = self
      resPredId = nodeHash
    }

    // Check if key lies between my pred and my hash
    currentKey = key
    var myId = nodeHash

    // If key is in this interval, return my predecessor
    if (checkRange(false,predecessorId,myId,false,currentKey)) {
      resPredRef = predecessor
      resPredId = predecessorId
    }

    // Find closest Id we can find in our finger table which lies before the key.
    val (closestPredRef, closestPredId) = findClosestPredInFT(key)
    // If we get a node other than self, we found a node closer to the key. Forward the req to get predecessor
    resPredRef = closestPredRef
    resPredId = closestPredId
    if (!(closestPredId == nodeHash)) {
      implicit val timeout: Timeout = Timeout(10.seconds)
      val future = closestPredRef ? CFindKeyPredecessor(key)
      val fingerNode = Await.result(future, timeout.duration).asInstanceOf[CFindKeyPredResponse]
      resPredRef = fingerNode.predRef
      resPredId = fingerNode.predId
      (resPredRef,resPredId)
    }
    else {
      (resPredRef,resPredId)
    }

  }

  def joinNetwork(existingNode: ActorRef): Unit = {

  }

  log.info("Classic actor created")
  override def receive: Receive = {
    case CFindKeyPredecessor(key) =>
      val (predRef, predId) = findKeyPredecessor(key)
      // println("inside FindKeyPredecessor. got "+predId.toString)
      sender ! CFindKeyPredResponse(predId, predRef)

    case CGetFingerTableStatus() =>
      sender ! CFingerTableStatusResponse(getFingerTableStatus())


    case CGetNodeSuccessor() =>
      sender ! CGetNodeSuccResponse(successorId,successor)

    case CSetNodeSuccessor(id,ref) =>
      successor = ref
      successorId = id
      fingerTable(0).nodeRef = ref
      fingerTable(0).nodeId = id

    case CSetNodePredecessor(id,ref) =>
      predecessor = ref
      predecessorId = id


    case CFindKeySuccessor(key) =>
      var succ: ActorRef = null
      val (predRef,predId)=findKeyPredecessor(key)
      //          println("inside FindKeySuccessor. Got pred "+predId.toString)
      if(predRef==self)
      { // key if found between this node and it successor
        sender ! CFindKeySuccResponse(successorId,successor,nodeHash,self)
      }
      else
      { // key is found between node pred and its successor. Get pred's successor and reply with their ref

        implicit val timeout: Timeout = Timeout(10.seconds)
        val future = predRef ? CGetNodeSuccessor()
        val nodeSuccessorResponse = Await.result(future, timeout.duration).asInstanceOf[CGetNodeSuccResponse]

        sender ! CFindKeySuccResponse(nodeSuccessorResponse.nodeId,nodeSuccessorResponse.nodeRef,predId,predRef)


      }

    case CUpdateFingerTable(ref,id,i,key) =>
      // Check if the candidate node is between ith start and ith finger node

      var ithFingerId = fingerTable(i).nodeId
      var candidateId = id
      // Check for Zero crossover between candidate node and current ith finger node
      if (checkRange(false,nodeHash,ithFingerId,false,candidateId)) {
        // Check for Zero crossover between candidate node and ith finger start
        var ithStart = fingerTable(i).start
        candidateId = id
        if (checkRange(false,nodeHash,candidateId,false,ithStart)) {
          fingerTable(i).nodeId = id
          fingerTable(i).nodeRef = ref
          // Also check if successor needs to be updated
          if (i == 0) {
            successorId = id
            successor = ref
          }
        }
        predecessor ! CUpdateFingerTable(ref,id,i,key)
      }

    case CJoinNetwork(networkRef) =>
      // We assume network has at least one node and so, networkRef is not null

      println("Join network called.")

      implicit val timeout: Timeout = Timeout(10.seconds)
      val future = networkRef ? CFindKeySuccessor(fingerTable(0).start)
      val successorResp = Await.result(future, timeout.duration).asInstanceOf[CFindKeySuccResponse]

      println("--- Finding succ --- Node :"+nodeHash.toString+" succ : "+successorResp.succId.toString)
      fingerTable(0).nodeRef = successorResp.succRef
      fingerTable(0).nodeId = successorResp.succId
      successor = successorResp.succRef
      successorId = successorResp.succId
      predecessor = successorResp.predRef
      predecessorId = successorResp.predId
      successor ! CSetNodePredecessor(nodeHash,self)
      predecessor ! CSetNodeSuccessor(nodeHash,self)
      for (i <- 1 until nodeConf.getInt("networkConstants.M")) {

        var lastSucc = fingerTable(i-1).nodeId
        var curStart = fingerTable(i).start

        if (checkRange(false,nodeHash,lastSucc,false,curStart)) {
          fingerTable(i).nodeId = fingerTable(i-1).nodeId
          fingerTable(i).nodeRef = fingerTable(i-1).nodeRef
        }
        else {

          implicit val timeout: Timeout = Timeout(10.seconds)
          val future = networkRef ? CFindKeySuccessor(curStart)
          val keySuccessorResp = Await.result(future, timeout.duration).asInstanceOf[CFindKeySuccResponse]
          fingerTable(i).nodeRef = keySuccessorResp.succRef
          fingerTable(i).nodeId = keySuccessorResp.succId

          }
        }
      println(getFingerTableStatus())
      updateFingerTablesOfOthers()
      log.info("{} added to chord network",nodeHash)

      sender() ! CJoinStatus("Success")


    case JoinNetwork(existingNode) => joinNetwork(existingNode)

    case CGetKeyValue(key: String) =>
      {
        println("Dummy value for "+key)
        sender() ! CDataResponse("Dummy value for "+key)
      }

    case CWriteKeyValue(key: String, value: String) =>
      {
        println("Received write request by classic chord node actor for:"+key+","+value)
        log.info("Received write request by classic chord node actor for:"+key+","+value)
      }
    case _ => log.info("Chord node actor recieved a generic message.")
  }

}
