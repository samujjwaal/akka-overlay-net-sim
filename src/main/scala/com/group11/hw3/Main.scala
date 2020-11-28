package com.group11.hw3

import scala.concurrent.duration._
import  akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await

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
    //chordMaster ! CreateNodes

    implicit val timeout: Timeout = Timeout(20.seconds)
    val future = chordMaster ? CreateNodes
    val createNodesReply = Await.result(future, timeout.duration).asInstanceOf[CreateNodesReply]

//    val classicActor=system.actorOf(ChordClassicNode.props(1),"chord-classic-actor")
//    classicActor ! "Start actor"

    val server = new HTTPServer()
    server.setupServer(chordSystem,createNodesReply.nodeHash)

    Thread.sleep(100)

//    val userSystem = ActorSystem(userConf.getString("userSystemName"))
//    val userMaster = userSystem.actorOf(UserMaster.props(),"user-master")
//    userMaster ! CreateUsers
//    userMaster ! StartUserRequests

//    sys ! CaptureGlobalSnapshot()

  }

}
