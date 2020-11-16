package com.group11.hw3.chord

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.util.Timeout
import com.group11.hw3._

import scala.math.pow

object ChordNode{
  val M = 30
  val ringSize: Int = pow(2,M).toInt
  private val fingerTable = new Array[Finger](M)

  def apply(nodeIndex: Int): Behavior[NodeCommand] = Behaviors.setup{ context =>

    var predecessor = context.self
    var successor = context.self
    implicit val timeout: Timeout = Timeout(10, TimeUnit.SECONDS)

    fingerTable.indices.foreach(i =>{
        val start = (nodeIndex + pow(2,i)).toInt % ringSize
        fingerTable(i) = Finger(start, context.self)
      } )

      Behaviors.receiveMessage[NodeCommand]{
        case FindPredecessor(key) =>
          context.log.info("predecessor {} was found at key {}",predecessor,key)
          Behaviors.same

        case FindSuccessor(key) =>
          context.log.info("successor {} was found at key {}",successor,key)
          Behaviors.same

        case GetNodeIndex() =>
          context.log.info("Node index is {}",nodeIndex)
          Behaviors.same

        case DisplayNodeInfo() =>
          fingerTable.foreach(i=>
          println(i.start, i.node))
          Behaviors.same
        case _ =>
          Behaviors.unhandled
      }
  }
}


object ChordSystem {
  def apply(): Behavior[NodeCommand] = Behaviors.setup { context =>
    val node = context.spawn(ChordNode(1),"Node_1")
    node ! FindPredecessor("k")
    node ! FindSuccessor("k")
    node ! GetNodeIndex()
    node ! DisplayNodeInfo()

    Behaviors.empty
  }
}

object NodeTest{
  def main(args: Array[String]): Unit = {
    val chordSystem = ActorSystem(ChordSystem(),"ChordServerSystem")
  }
}
