package com.group11.hw3

import akka.actor.ActorSystem
import com.group11.hw3.chord.ChordClassicNode

/*
Class used to test the basic working of the akka http server.
 */
object Main {
  def main(args: Array[String]): Unit = {

    //ActorSystem(ChordHttpServer(),"ChordServerSystem")
    //Thread.sleep(2000)
    //println("Starting user requests now...")
    //ActorSystem(UserSystem(),"user-system")

    val system= ActorSystem("ChordActorSystemClassic")
    val classicActor=system.actorOf(ChordClassicNode.props(1),"chord-classic-actor")
    classicActor ! "Start actor"

    val userSystem = ActorSystem("UserActorSystem")
    val userMaster = userSystem.actorOf(UserMaster.props())
    userMaster ! UserMaster.CreateUsers
    userMaster ! UserMaster.StartUserRequests

//    sys ! CaptureGlobalSnapshot()

  }

}
