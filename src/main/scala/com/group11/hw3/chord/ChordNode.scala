package com.group11.hw3.chord

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.group11.hw3._
import com.group11.hw3.utils.ChordUtils.md5
import java.util.concurrent.TimeUnit

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

    val selfRef = context.self
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

    }
    override def onMessage(msg: NodeCommand): Behavior[NodeCommand] =
      msg match {
        case FindKeyPredecessor(replyTo,key) =>
//          context.log.info("predecessor {} was found at key {}",predecessor,key)
          Behaviors.same

        case FindKeySuccessor(replyTo,key) =>
//          context.log.info("for node {} successor {}",context.self,this.successor)
//          println(s"for node ${context.self} successor ${this.successor}")

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

        case UpdateFingerTable() =>
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
          implicit val timeout = Timeout(5 seconds)
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
