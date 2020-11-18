package com.group11.hw3.chord

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.group11.hw3._
import com.group11.hw3.utils.ChordUtils.md5

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.collection.mutable

object ChordNode{
  val M = 30
  val ringSize: BigInt = BigInt(2).pow(M)
//  private val fingerTable = new Array[Finger](M)
//  var predecessor: ActorRef[NodeCommand] = _
//  var successor: ActorRef[NodeCommand] = _

  def apply(nodeHash: BigInt): Behavior[NodeCommand] = Behaviors.setup{ context =>

//    predecessor = context.self
//    successor = context.self
//    val nodeHash = hash

//    fingerTable.indices.foreach(i =>{
//        val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
//        fingerTable(i) = Finger(start, context.self)
//      } )

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

//    def this(){
//      this(context,nodeHash)
//      this.predecessor = context.self
//      this.successor = context.self
//      this.fingerTable.indices.foreach(i =>{
//        val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
//        fingerTable(i) = Finger(start, context.self)
//      } )
//    }
    def updateFingerTablesOfOthers(): Unit = {
      for (i <- 0 until M) {
        var p = (nodeHash - BigInt(2).pow(i) + BigInt(2).pow(M) + 1) % BigInt(2).pow(M)
        var (pred,predID)=findKeyPredecessor(p)
        pred ! UpdateFingerTable(selfRef,nodeHash,i,p)
      }
    }

    def findClosestPreceedingId(identifier:BigInt, predRef:ActorRef[NodeCommand],nodeID:BigInt,call:String):(ActorRef[NodeCommand],BigInt) =
    {
      var predNode = selfRef;
      var predNodeID: BigInt = nodeHash

      call match {
        case "Join" =>
          if (identifier == nodeID) {
            return (predecessor, predecessorId)
          }
          for (i <- M - 1 to 0 by -1) {

            var intervalEnd = identifier
            var valueToFind = fingerTable(i).start
            if (identifier.<=(nodeID)) {
              intervalEnd = identifier + Math.pow(2, M).toInt
              if (fingerTable(i).nodeId.<=(nodeID)) {
                valueToFind = fingerTable(i).nodeId + Math.pow(2, M).toInt
              }
            }

            if (valueToFind.>(nodeID) && valueToFind.<(intervalEnd)) {
              return (fingerTable(i).nodeRef, fingerTable(i).nodeId)
            }

          }

      }

      (predNode,predNodeID)
    }
    def findKeyPredecessor(identifier:BigInt): (ActorRef[NodeCommand], BigInt) =
    {
      var predNode = selfRef
      var predNodeID: BigInt = nodeHash
      var intervalEnd = successorId
      var valueToFind = identifier

      if(intervalEnd.<(nodeHash)){
        intervalEnd = successorId + BigInt(2).pow(M)
        if(valueToFind.<(nodeHash)){
          valueToFind = identifier + BigInt(2).pow(M)
        }
      }

      if(!(valueToFind.>(nodeHash) && valueToFind.<=(intervalEnd))){
        val (predNode1, predNodeID1) = findClosestPreceedingId(identifier, predNode, nodeHash, "Join")

        if(!(predNode1 == selfRef)){
          implicit val timeout: Timeout = Timeout(10.seconds)
          def getKeyPred(ref: ActorRef[NodeCommand]) = CallFindPredecessor(ref, identifier)
          context.ask(predNode1,getKeyPred){
            case Success(CallFindPredResponse(predID, pred)) =>
              return (pred, predID)
          }
        }
        else {
          return (predNode1, predNodeID1)
        }
      }

      (predNode,predNodeID)
    }
    override def onMessage(msg: NodeCommand): Behavior[NodeCommand] =
      msg match {
        case CallFindPredecessor(replyTo, key)=>
          val (predNode1, predNodeID1) = findKeyPredecessor(key)
          replyTo ! CallFindPredResponse(predNodeID1, predNode1)
          Behaviors.same

        case GetNodeSuccessor(replyTo) =>
          replyTo ! GetNodeSuccResponse(successorId,successor)
          Behaviors.same

        case FindKeyPredecessor(replyTo,key) =>
          Behaviors.same

        case FindKeySuccessor(replyTo,key) =>

          var succ: ActorRef[NodeCommand] = null
          val (pred,predID)=findKeyPredecessor(key)
          if(pred==selfRef)
            {
              succ=successor
              replyTo ! FindKeySuccResponse(successorId,succ,predID,pred)
            }
          else
          {
            implicit val timeout: Timeout = Timeout(10 seconds)
            def getKeySucc(ref:ActorRef[NodeCommand]) = GetNodeSuccessor(ref)
            context.ask(pred,getKeySucc)
            {
              case Success(GetNodeSuccResponse(successorId_,successor)) =>
                succ=successor
                replyTo ! FindKeySuccResponse(successorId_,succ,predID,pred)
                NodeAdaptedResponse()
            }

          }


          Behaviors.same

        case GetNodeIndex() =>
          context.log.info("Node index is {}")
          Behaviors.same

        case DisplayNodeInfo() =>
          //          fingerTable.foreach(i=>
          //          println(i.start, i.node))
          println(context.self.path.name,fingerTable(0))
          println(context.self.path.name,fingerTable(1))
          println(context.self.path.name,fingerTable(2))
          println(context.self.path.name,fingerTable(3))

          println()
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

        case UpdateFingerTable(ref,id,index,key) =>
//          updateFingerTable()
          Behaviors.same

        case getKeyValue(replyTo,key) =>
          context.log.info("{} received read request by NODE ACTOR for key: {}", context.self.path.name, key)
          replyTo ! HttpResponse("Dummy value!")
          Behaviors.same

        case writeKeyValue(key,value) =>
          context.log.info("{} received write request by NODE ACTOR for key: {}, value: {}", context.self.path.name, key, value)
          Behaviors.same

        case JoinNetwork(replyTo,networkRef) =>
          // We assume network has at least one node and so, networkRef is not null
          implicit val timeout: Timeout = Timeout(5 seconds)
          def askForKeySucc(ref:ActorRef[NodeCommand]) = FindKeySuccessor(ref,fingerTable(0).start)
          context.ask(networkRef,askForKeySucc)
          {
            case Success(FindKeySuccResponse(succId,succRef,predId,predRef)) => {
              fingerTable(0).nodeRef = succRef
              fingerTable(0).nodeId = succId
              successor = succRef
              successorId = succId
              predecessor = predRef
              predecessorId = predId
              NodeAdaptedResponse()
            }
            case Failure(_) => {
              fingerTable(0).nodeRef = selfRef
              fingerTable(0).nodeId = nodeHash
              successor = selfRef
              successorId = nodeHash
              predecessor = selfRef
              predecessorId = nodeHash
              NodeAdaptedResponse()
            }
          }
          successor ! SetNodePredecessor(nodeHash,selfRef)
          predecessor ! SetNodeSuccessor(nodeHash,selfRef)

          // Set the Finger table with current nodes in the network
          for (i <- 1 until M) {
            var lastSucc = fingerTable(i-1).nodeId
            var curStart = fingerTable(i).start
            if (lastSucc.<(nodeHash)) {
              lastSucc = lastSucc + BigInt(2).pow(M)
              if (curStart.<(nodeHash)) {
                curStart = curStart + BigInt(2).pow(M)
              }
            }
            if (lastSucc.>=(curStart)) {
              fingerTable(i).nodeId = fingerTable(i-1).nodeId
              fingerTable(i).nodeRef = fingerTable(i-1).nodeRef
            }
            else {
              def askForNextKeySucc(ref:ActorRef[NodeCommand]) = FindKeySuccessor(ref,curStart)
              context.ask(networkRef,askForNextKeySucc) {
                case Success(FindKeySuccResponse(succId,succRef,predId,predRef)) => {
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
          updateFingerTablesOfOthers()
          replyTo ! JoinStatus("Success")

          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
  }
}


object ChordSystem {
  def apply(): Behavior[NodeCommand] = Behaviors.setup { context =>
    val node1 = context.spawn(ChordNode(md5("Node1")),"Node1")
    node1 ! DisplayNodeInfo()

    Thread.sleep(100)

    val node2 = context.spawn(ChordNode(md5("Node2")),"Node2")
//    node1 ! SetSuccessor(node2)
//    node1 ! FindSuccessor(md5("Node1"))
//    node2 ! DisplayNodeInfo()
//    node2 ! FindSuccessor(md5("Node2"))
//    Thread.sleep(100)
//    node1 ! DisplayNodeInfo()
//    val node3 = context.spawn(ChordNode(md5("Node3")),"Node3")
////    node2 ! SetSuccessor(node3)
////    node2 ! FindSuccessor()
//    node2 ! DisplayNodeInfo()
//    val node4 = context.spawn(ChordNode(md5("Node4")),"Node4")
//    node4 ! DisplayNodeInfo()

    Behaviors.empty
  }
}

object NodeTest{
  def main(args: Array[String]): Unit = {
    val chordSystem = ActorSystem(ChordSystem(),"ChordServerSystem")
  }
}
