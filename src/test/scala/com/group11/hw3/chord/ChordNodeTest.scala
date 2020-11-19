package com.group11.hw3.chord

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.group11.hw3.NodeCommand
import com.group11.hw3.utils.ChordUtils.md5
import org.scalatest.funsuite.AnyFunSuite

class ChordNodeTest extends AnyFunSuite {



  test("") {
    object TestChordSystem {
      def apply(): Behavior[NodeCommand] = Behaviors.setup { context =>

        val node1 = context.spawn(ChordNode(md5("Node1")), "Node1")
//        node1 ! DisplayNodeInfo()
        assert(node1.isInstanceOf[ActorRef[NodeCommand]])
        Thread.sleep(100)

        val node2 = context.spawn(ChordNode(md5("Node2")), "Node2")
//        node1 ! SetSuccessor(node2)
        //        node1 ! FindSuccessor()
//        node2 ! DisplayNodeInfo()
        //        node2 ! FindSuccessor()
        Thread.sleep(100)
//        node1 ! DisplayNodeInfo()

        //        val node3 = context.spawn(ChordNode2(md5("Node3")),"Node3")
        //        node2 ! SetSuccessor(node3)
        //        node2 ! FindSuccessor()
        //        node3 ! DisplayNodeInfo()
        //        node3 ! FindSuccessor()
        Thread.sleep(100)
//        node2 ! DisplayNodeInfo()
        //    val node4 = context.spawn(ChordNode2(md5("Node4")),"Node4")
        //    node4 ! DisplayNodeInfo()

        Behaviors.stopped
      }
    }

    val chordSystem = ActorSystem(TestChordSystem(),"TestChordSystem")
    assert(chordSystem.isInstanceOf[ActorSystem[NodeCommand]])

  }
}
