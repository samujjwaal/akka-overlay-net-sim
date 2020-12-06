package com.group11.can

import akka.actor.{ActorRef, ActorSystem}
import com.group11.can.CanMessageTypes.JoinCan
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable

object CANmain {
  def main(args: Array[String]): Unit = {
    val conf: Config = ConfigFactory.load("application.conf")
    val netConf = conf.getConfig("CANnetworkConstants")

    val chordSystem = ActorSystem(netConf.getString("CANSystemName"))
    val numNodes = netConf.getInt(("numNodes"))
    val bootstrap = new mutable.HashMap[BigInt,ActorRef]()
    var id = BigInt(0)
    val newNode = chordSystem.actorOf(CanNode.props(id),id.toString)
    bootstrap.addOne(id,newNode)
    newNode ! JoinCan(bootstrap(0))
    Thread.sleep(10)
    while (id < numNodes) {
      id += 1
      val newNode = chordSystem.actorOf(CanNode.props(id),id.toString)
      val peer = bootstrap(scala.util.Random.nextInt(bootstrap.size))
      bootstrap.addOne(id,newNode)
      newNode ! JoinCan(peer)
      Thread.sleep(100)
    }


  }

}
