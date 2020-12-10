package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.group11.hw3.chord.ChordClassicNode
import org.scalatest.funsuite.AnyFunSuite

import scala.language.postfixOps

class ChordTest extends AnyFunSuite {

  val testSystem: ActorSystem = ActorSystem("TestChordSystem")

  assert(testSystem.isInstanceOf[ActorSystem])


  test("Test Chord Node Actor Creation") {

    case class TestMessage()

    val node1 = testSystem.actorOf(Props[ChordClassicNode](), "Node1")
    assert(node1.isInstanceOf[ActorRef])
    node1 ! TestMessage

    val node2 = testSystem.actorOf(Props[ChordClassicNode](), "Node2")
    assert(node2.isInstanceOf[ActorRef])
    assert(node1!=node2)

  }

  test("Test Cluster Shard Region creation"){
    val chordShardRegion: ActorRef = ClusterSharding(testSystem).start(
      typeName = "ChordNodeRegion",
      entityProps = Props[ChordClassicNode](),
      settings = ClusterShardingSettings(testSystem),
      extractEntityId = ChordClassicNode.extractEntityId,
      extractShardId = ChordClassicNode.extractShardId
    )
    assert(chordShardRegion.isInstanceOf[ActorRef])
  }
}
