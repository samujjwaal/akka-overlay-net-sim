package com.group11.hw3.chord

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.group11.hw3._
import com.group11.hw3.utils.ChordUtils.md5
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.language.postfixOps

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

    private val fingerTable = new Array[Finger](M)
    var predecessor: ActorRef[NodeCommand] = context.self
    var successor: ActorRef[NodeCommand] = context.self

    fingerTable.indices.foreach(i =>{
      val start:BigInt = (nodeHash + BigInt(2).pow(i)) % ringSize
      fingerTable(i) = Finger(start, context.self, nodeHash)
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
        fingerTable(i) = Finger(start, context.self,nodeHash)
      } )
    }
    override def onMessage(msg: NodeCommand): Behavior[NodeCommand] =
      msg match {
        case FindPredecessor(key) =>
          context.log.info("predecessor {} was found at key {}",predecessor,key)
          Behaviors.same

        case FindSuccessor() =>
//          context.log.info("for node {} successor {}",context.self,this.successor)
          println(s"for node ${context.self} successor ${this.successor}")
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

        case SetSuccessor(node) =>
          successor = node
          fingerTable(0).nodeRef = node
          Behaviors.same


        case UpdateFingerTable() =>
          updateFingerTable()
          Behaviors.same

        case getKeyValue(replyTo,key) =>
          context.log.info("{} received read request by NODE ACTOR for key: {}", context.self.path.name, key)
          replyTo ! Response("Dummy value!")
          Behaviors.same

        case writeKeyValue(key,value) =>
          context.log.info("{} received write request by NODE ACTOR for key: {}, value: {}", context.self.path.name, key, value)
          Behaviors.same

        case JoinNetwork(networkRef,master) =>
          // We assume network has at least one node and so, networkRef is not null
          implicit val timeout = Timeout(10 seconds)

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
    node1 ! SetSuccessor(node2)
    node1 ! FindSuccessor()
    node2 ! DisplayNodeInfo()
    node2 ! FindSuccessor()
    Thread.sleep(100)
    node1 ! DisplayNodeInfo()
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
