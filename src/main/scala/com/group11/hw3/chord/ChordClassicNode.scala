package com.group11.hw3.chord

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable

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
  val ringSize= BigInt(2).pow(nodeConf.getInt("nodeConstants.M"))

  var successor:ActorRef= self
  var predecessor:ActorRef=self

  var predecessorId: BigInt = nodeHash
  var successorId: BigInt = nodeHash

  var nodeData = new mutable.HashMap[BigInt,Int]()
  var fingerTable = new Array[ClassicFinger](nodeConf.getInt("nodeConstants.M"))

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

//      val future = closestPredRef ? FindKeyPredecessor(key)
//      val fingerNode = Await.result(future, timeout.duration).asInstanceOf[FingerNodeRef]
      //implicit val timeout: Timeout = Timeout(10.seconds)

//      def getKeyPred(ref: ActorRef[NodeCommand]) = FindKeyPredecessor(ref, key)

//      context.ask(closestPredRef, getKeyPred) {
//        case Success(FindKeyPredResponse(predID, pred)) =>
//          //            println("---- FindKeyPredecessor ask call ----"+predID.toString)
//          resPredRef = pred
//          resPredId = predID
//          NodeAdaptedResponse()
//        case Failure(_) =>
//          println("Failed to get Key predecessor")
//          NodeAdaptedResponse()
//      }
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
    case JoinNetwork(existingNode) => joinNetwork(existingNode)
    case _ => log.info("Actor recieved message.")
  }

}
