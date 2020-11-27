package com.group11.hw3

import akka.actor.ActorSystem
import com.group11.hw3.chord.ChordClassicNode
import com.typesafe.config.{Config, ConfigFactory}

/*
Class used to test the basic working of the akka http server.
 */
object Main {
  def main(args: Array[String]): Unit = {

    val conf: Config = ConfigFactory.load("application.conf")
    val netConf = conf.getConfig("networkConstants")
    val userConf = conf.getConfig("userConstants")

    //ActorSystem(ChordHttpServer(),"ChordServerSystem")
    //Thread.sleep(2000)
    //println("Starting user requests now...")
    //ActorSystem(UserSystem(),"user-system")

    val chordSystem = ActorSystem(netConf.getString("networkSystemName"))
    val chordMaster = chordSystem.actorOf(ChordMaster.props())
    chordMaster ! ChordMaster.CreateNodes
//    val classicActor=system.actorOf(ChordClassicNode.props(1),"chord-classic-actor")
//    classicActor ! "Start actor"

    val userSystem = ActorSystem(userConf.getString("userSystemName"))
    val userMaster = userSystem.actorOf(UserMaster.props())
    userMaster ! UserMaster.CreateUsers
    userMaster ! UserMaster.StartUserRequests

//    sys ! CaptureGlobalSnapshot()

  }

}
