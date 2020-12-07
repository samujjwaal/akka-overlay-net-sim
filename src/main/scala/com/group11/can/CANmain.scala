package com.group11.can

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.group11.can.CanMessageTypes.{JoinCan, JoinDone, PrintNeighbors}
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object CANmain {
  def main(args: Array[String]): Unit = {
    val conf: Config = ConfigFactory.load("application.conf")
    val netConf = conf.getConfig("CANnetworkConstants")

    val canNodesRef = new mutable.HashMap[BigInt,ActorRef]()

    val canSystem = ActorSystem(netConf.getString("CANSystemName"))
    val numNodes = netConf.getInt(("numNodes"))
    val bootstrap = new mutable.HashMap[BigInt,ActorRef]()
    var id = BigInt(0)
    val newNode = canSystem.actorOf(CanNode.props(id),id.toString)
    bootstrap.addOne(id,newNode)

    implicit val timeout = Timeout(10 seconds)
    val future= newNode ? JoinCan(bootstrap(0))
    val joinStatus = Await.result(future,timeout.duration).asInstanceOf[JoinDone]



    Thread.sleep(10)
    canNodesRef.addOne(id,newNode)
    while (id < numNodes-1) {
      id += 1
      val newNode = canSystem.actorOf(CanNode.props(id),id.toString)
      val peer = bootstrap(scala.util.Random.nextInt(bootstrap.size))
      bootstrap.addOne(id,newNode)
      implicit val timeout = Timeout(20 seconds)
      val future =newNode ? JoinCan(peer)
      val joinStatus = Await.result(future,timeout.duration).asInstanceOf[JoinDone]
      canNodesRef.addOne(id,newNode)
      //newNode ! JoinCan(peer)
      Thread.sleep(100)
    }

    Thread.sleep(2000)
    for(node <- canNodesRef)
      {
        node._2! PrintNeighbors
        Thread.sleep(10)
      }

  }

}
