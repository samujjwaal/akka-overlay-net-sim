package com.group11.hw3.chord

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.group11.hw3._
import com.group11.hw3.utils.ChordUtils.md5

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

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
    def updateFingerTable(): Unit = {
      fingerTable.indices.foreach ( i => {
        val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
        val nextSuccessor = context.self
        fingerTable(i) = Finger(start, selfRef,nodeHash)
      } )
    }

    def findPredecessor(identifier:BigInt) =
    {
      var predNode = selfRef;
      var predNodeID: BigInt = nodeHash

      (predNode,predNodeID)
    }
    override def onMessage(msg: NodeCommand): Behavior[NodeCommand] =
      msg match {
        case GetKeySuccessor(replyTo) =>
          replyTo ! GetKeySuccessorResponse(successorId,successor)
          Behaviors.same

        case FindKeyPredecessor(replyTo,key) =>
          Behaviors.same

        case FindKeySuccessor(replyTo,key) =>

          var succ: ActorRef[NodeCommand] = null;
          var (pred,predID)=findPredecessor(key)
          if(pred==selfRef)
            {
              succ=successor
              replyTo ! FindKeySuccResponse(successorId,succ,predID,pred)
            }
          else
          {
            implicit val timeout = Timeout(10 seconds)
            def getKeySucc(ref:ActorRef[NodeCommand]) = GetKeySuccessor(ref)
            context.ask(pred,getKeySucc)
            {
              case Success(GetKeySuccessorResponse(successorId_,successor)) =>
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

        case UpdateFingerTable() =>
          updateFingerTable()
          Behaviors.same

        case getKeyValue(replyTo,key) =>
          context.log.info("{} received read request by NODE ACTOR for key: {}", context.self.path.name, key)
          replyTo ! HttpResponse("Dummy value!")
          Behaviors.same

        case writeKeyValue(key,value) =>
          context.log.info("{} received write request by NODE ACTOR for key: {}, value: {}", context.self.path.name, key, value)
          Behaviors.same

        case JoinNetwork(networkRef,master) =>
          // We assume network has at least one node and so, networkRef is not null
          implicit val timeout = Timeout(10 seconds)
          def askForKeySucc(ref:ActorRef[NodeCommand]) = FindKeySuccessor(ref,fingerTable(0).start)
          var succId = BigInt(0)
          var predId = BigInt(0)
          var succRef: ActorRef[NodeCommand] = null
          var predRef: ActorRef[NodeCommand] = null
          context.ask(networkRef,askForKeySucc)
          {
            case Success(FindKeySuccResponse(succId_,succRef_,predId_,predRef_)) => {
              succId = succId_
              predId = predId_
              succRef = succRef_
              predRef = predRef_
              NodeAdaptedResponse()
            }
            case Failure(_) => {
              succId = nodeHash
              predId = nodeHash
              succRef = selfRef
              predRef = selfRef
              NodeAdaptedResponse()
            }
          }
          fingerTable(0).nodeRef = succRef
          fingerTable(0).nodeId = succId
          successor = succRef
          successorId = succId
          predecessor = predRef
          predecessorId = predId
          successor ! SetNodePredecessor(nodeHash,selfRef)
          predecessor ! SetNodeSuccessor(nodeHash,selfRef)

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
