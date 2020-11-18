package com.group11.hw3.chord

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import com.google.gson.JsonObject
import com.group11.hw3._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ChordNode{
  val M = NodeConstants.M
  val ringSize: BigInt = BigInt(2).pow(M)

  def apply(nodeHash: BigInt): Behavior[NodeCommand] = Behaviors.setup{ context =>
    new ChordNodeBehavior(context, nodeHash)
  }

  class ChordNodeBehavior(context: ActorContext[NodeCommand], nodeHash: BigInt) extends AbstractBehavior[NodeCommand](context){

    val selfRef: ActorRef[NodeCommand] = context.self
    private val fingerTable = new Array[Finger](M)
    var predecessor: ActorRef[NodeCommand] = selfRef
    var successor: ActorRef[NodeCommand] = selfRef
    var predecessorId: BigInt = nodeHash
    var successorId: BigInt = nodeHash
    var nodeData = new mutable.HashMap[BigInt,Int]()

    fingerTable.indices.foreach(i =>{
      val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
      fingerTable(i) = Finger(start, selfRef, nodeHash)
    } )

    println("Node Created : "+nodeHash.toString+" Initial Finger table on node creation : "+getFingerTableStatus())

    def getFingerTableStatus(): String = {
      var fingerTableStatus = "[ "
      for (finger <- fingerTable) {
        fingerTableStatus = fingerTableStatus+"( "+finger.start.toString+" : "+finger.nodeId.toString+" ), "
      }
      fingerTableStatus = fingerTableStatus + "]"
      fingerTableStatus
    }

    def updateFingerTablesOfOthers(): Unit = {
      for (i <- 0 until M) {
        var p = (nodeHash - BigInt(2).pow(i) + BigInt(2).pow(M) + 1) % BigInt(2).pow(M)
        var (pred,predID)=findKeyPredecessor(p)
        pred ! UpdateFingerTable(selfRef,nodeHash,i,p)
      }
    }

    def findClosestPredInFT(key:BigInt):(ActorRef[NodeCommand],BigInt) =
    {
      var resPredRef: ActorRef[NodeCommand] = selfRef
      var resPredId: BigInt = nodeHash
      // If my hash is equal to the key, return my predecessor
      if (key == nodeHash) {
        resPredRef = selfRef
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
            if (currKey.<(nodeHash)) {
              currKey = currKey + BigInt(2).pow(M)
              if (fingernode.<=(nodeHash)) {
                fingernode = fingernode + BigInt(2).pow(M)
              }
            }
            if (fingernode.>(nodeHash) && fingernode.<(currKey)) {
              resPredRef = fingerTable(index).nodeRef
              resPredId = fingerTable(index).nodeId
            }
          }
        }
      }
      // If no closest node found in the finger table, return self
      (resPredRef,resPredId)
    }

    def findKeyPredecessor(key:BigInt): (ActorRef[NodeCommand], BigInt) = {
      var resPredRef: ActorRef[NodeCommand] = selfRef
      var resPredId: BigInt = nodeHash
      // If my hash is the same as key, return my predecessor
      if (key == nodeHash) {
        resPredRef = predecessor
        resPredId = predecessorId
      }
      // If key == my successor, return self
      if (key == successorId) {
        resPredRef = selfRef
        resPredId = nodeHash
      }

      // Check if the key lies between my hash and my successor's hash
      var succId = successorId
      var currentKey = key
      if (succId.<(nodeHash)) {
        succId = succId + BigInt(2).pow(M)
        if (currentKey.<(nodeHash)) {
          currentKey = currentKey + BigInt(2).pow(M)
        }
      }
      // If key is in the interval, return self as the predecessor
      if (currentKey.>(nodeHash) && currentKey.<(succId)) {
        resPredRef = selfRef
        resPredId = nodeHash
      }

      // Check if key lies between my pred and my hash
      currentKey = key
      var myId = nodeHash
      if (myId.<(predecessorId)) {
        myId = myId + BigInt(2).pow(M)
        if (currentKey.<(predecessorId)) {
          currentKey = currentKey + BigInt(2).pow(M)
        }
      }
      // If key is in this interval, return my predecessor
      if (currentKey.>(predecessorId) && currentKey.<(myId)) {
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

        def getKeyPred(ref: ActorRef[NodeCommand]) = FindKeyPredecessor(ref, key)

        context.ask(closestPredRef, getKeyPred) {
          case Success(FindKeyPredResponse(predID, pred)) =>
//            println("---- FindKeyPredecessor ask call ----"+predID.toString)
            resPredRef = pred
            resPredId = predID
            NodeAdaptedResponse()
          case Failure(_) =>
            println("Failed to get Key predecessor")
            NodeAdaptedResponse()
        }
        (resPredRef,resPredId)
      }
      else {
        (resPredRef,resPredId)
      }

    }


    override def onMessage(msg: NodeCommand): Behavior[NodeCommand] =
      msg match {

        case GetNodeSnapshot(replyTo) =>
          val fingerJson: JsonObject = new JsonObject
          for (i <- fingerTable.indices) {
            fingerJson.addProperty(i.toString, fingerTable(i).nodeId)
          }
          val nodeJson: JsonObject = new JsonObject
          nodeJson.addProperty("Node", nodeHash)
          nodeJson.addProperty("Successor", successor.path.name)
          nodeJson.addProperty("Predecessor", predecessor.path.name)
          nodeJson.addProperty("KeyValuePairs", nodeData.size)
          nodeJson.add("Fingers", fingerJson)
          replyTo ! GetNodeSnapshotResponse(nodeJson)
          Behaviors.same

        case GetFingerTableStatus(replyTo) =>
          replyTo ! FingerTableStatusResponse(getFingerTableStatus())
          Behaviors.same

        case GetNodeSuccessor(replyTo) =>
          replyTo ! GetNodeSuccResponse(successorId,successor)
          Behaviors.same

        case SetNodeSuccessor(id,ref) =>
          successor = ref
          successorId = id
          fingerTable(0).nodeRef = ref
          fingerTable(0).nodeId = id
          Behaviors.same

        case SetNodePredecessor(id,ref) =>
          predecessor = ref
          predecessorId = id
          Behaviors.same

        case FindKeyPredecessor(replyTo,key) =>
          val (predRef, predId) = findKeyPredecessor(key)
//          println("inside FindKeyPredecessor. got "+predId.toString)
          replyTo ! FindKeyPredResponse(predId, predRef)
          Behaviors.same

        case FindKeySuccessor(replyTo,key) =>
          var succ: ActorRef[NodeCommand] = null
          val (predRef,predId)=findKeyPredecessor(key)
//          println("inside FindKeySuccessor. Got pred "+predId.toString)
          if(predRef==selfRef)
            { // key if found between this node and it successor
              replyTo ! FindKeySuccResponse(successorId,successor,nodeHash,selfRef)
            }
          else
          { // key is found between node pred and its successor. Get pred's successor and reply with their ref
            implicit val timeout: Timeout = Timeout(10 seconds)
            def getNodeSucc(ref:ActorRef[NodeCommand]) = GetNodeSuccessor(ref)
            context.ask(predRef,getNodeSucc)
            {
              case Success(GetNodeSuccResponse(succId,succ)) =>
                replyTo ! FindKeySuccResponse(succId,succ,predId,predRef)
                NodeAdaptedResponse()
              case Failure(_) =>
                println("Failed to get Node Successor")
                NodeAdaptedResponse()
            }
          }
          Behaviors.same

        case UpdateFingerTable(ref,id,i,key) =>
          // Check if the candidate node is between ith start and ith finger node

          var ithFingerId = fingerTable(i).nodeId
          var candidateId = id
          // Check for Zero crossover between candidate node and current ith finger node
          if (ithFingerId.<(nodeHash)) {
            ithFingerId = ithFingerId + BigInt(2).pow(M)
            if (candidateId.<(nodeHash)) {
              candidateId = candidateId + BigInt(2).pow(M)
            }
          }
          if (candidateId.>(nodeHash) && ithFingerId.>(candidateId)) {
            // Check for Zero crossover between candidate node and ith finger start
            var ithStart = fingerTable(i).start
            candidateId = id
            if (candidateId.<(nodeHash)) {
              candidateId = candidateId + BigInt(2).pow(M)
              if (ithStart.<(nodeHash)) {
                ithStart = ithStart + BigInt(2).pow(M)
              }
            }
            if (ithStart.>(nodeHash) && ithStart.<(candidateId)) {
              fingerTable(i).nodeId = id
              fingerTable(i).nodeRef = ref
              // Also check if successor needs to be updated
              if (i == 0) {
                successorId = id
                successor = ref
              }
            }
            predecessor ! UpdateFingerTable(ref,id,i,key)
          }

          Behaviors.same

        case GetKeyValue(replyTo,key) =>
          context.log.info("{} received read request by NODE ACTOR for key: {}", context.self.path.name, key)
          replyTo ! DataResponse("Dummy value!")
          Behaviors.same

        case WriteKeyValue(key,value) =>
          context.log.info("{} received write request by NODE ACTOR for key: {}, value: {}", context.self.path.name, key, value)
          Behaviors.same

        case JoinNetwork(replyTo,networkRef) =>
          // We assume network has at least one node and so, networkRef is not null
          implicit val timeout: Timeout = Timeout(5 seconds)
          def askForKeySucc(ref:ActorRef[NodeCommand]) = FindKeySuccessor(ref,fingerTable(0).start)
          context.ask(networkRef,askForKeySucc)
          {
            case Success(FindKeySuccResponse(succId,succRef,predId,predRef)) => {
              println("--- Finding succ --- Node :"+nodeHash.toString+" succ : "+succId.toString)
              fingerTable(0).nodeRef = succRef
              fingerTable(0).nodeId = succId
              successor = succRef
              successorId = succId
              predecessor = predRef
              predecessorId = predId
              successor ! SetNodePredecessor(nodeHash,selfRef)
              predecessor ! SetNodeSuccessor(nodeHash,selfRef)
//              println(getFingerTableStatus())
              // Set the Finger table with current nodes in the network
//              var gotFingerNode = new ListBuffer[Int]()
//              gotFingerNode += 1
              for (i <- 1 until M) {

                var lastSucc = fingerTable(i-1).nodeId
                var curStart = fingerTable(i).start
                if (lastSucc.<(nodeHash)) {
                  lastSucc = lastSucc + BigInt(2).pow(M)
                  if (curStart.>(nodeHash) && curStart.<(nodeHash)) {
                    curStart = curStart + BigInt(2).pow(M)
                  }
                }
                if (lastSucc.>(nodeHash) && lastSucc.>=(curStart)) {
                  fingerTable(i).nodeId = fingerTable(i-1).nodeId
                  fingerTable(i).nodeRef = fingerTable(i-1).nodeRef
//                  gotFingerNode += 1
                }
                else {
                  def askForNextKeySucc(ref:ActorRef[NodeCommand]) = FindKeySuccessor(ref,curStart)
                  context.ask(networkRef,askForNextKeySucc) {
                    case Success(FindKeySuccResponse(succId,succRef,predId,predRef)) => {
//                      gotFingerNode(i) += 1
                      fingerTable(i).nodeRef = succRef
                      fingerTable(i).nodeId = succId
                      NodeAdaptedResponse()
                    }
                    case Failure(_) => {
                      fingerTable(i).nodeRef = selfRef
                      fingerTable(i).nodeId = nodeHash
                      NodeAdaptedResponse()
                    }
                  }
                }
              }
//              Thread.sleep(10)
//              while (gotFingerNode.size < M) {
//                Thread.sleep((1))
//              }
              updateFingerTablesOfOthers()
              context.log.info("{} added to chord network",nodeHash)

//              context.log.info("Fingertable for {} => {}",nodeHash,getFingerTableStatus())
              replyTo ! JoinStatus("Success")
              NodeAdaptedResponse()
            }
            case Failure(_) => {
              replyTo ! JoinStatus("Failed")
              NodeAdaptedResponse()
            }
          }
          Behaviors.same

        case NodeAdaptedResponse() =>
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
  }
}
