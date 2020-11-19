package com.group11.hw3.chord

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.group11.hw3.NodeCommand
import com.group11.hw3.utils.ChordUtils.md5
import org.scalatest.funsuite.AnyFunSuite

class ChordNodeTest extends AnyFunSuite {



  test("Chord Node Actor") {
    object TestChordSystem {
      def apply(): Behavior[NodeCommand] = Behaviors.setup { context =>

        val node1 = context.spawn(ChordNode(md5("Node1")), "Node1")

        assert(node1!=null)
        assert(node1.isInstanceOf[ActorRef[NodeCommand]])

        Thread.sleep(100)

        val node2 = context.spawn(ChordNode(md5("Node2")), "Node2")

        assert(node1.path != node2.path)

        Behaviors.stopped
      }
    }

    val chordSystem = ActorSystem(TestChordSystem(),"TestChordSystem")

    assert(chordSystem.isInstanceOf[ActorSystem[NodeCommand]])

  }
}
