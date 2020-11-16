package com.group11.hw3

import akka.http.scaladsl.server.Directives._
import akka.NotUsed
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, path, post}
import com.group11.hw3.utils.ChordUtils

import scala.collection.mutable
import scala.concurrent.ExecutionContext

object serverRedone {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    var hashMap = new mutable.HashMap[String,ActorRef[NodeRequest]]()
    val nodeList = new Array[String](11)

    implicit val system:ActorSystem[NotUsed]=ActorSystem(Behaviors.empty,"http-server")
    implicit val executor: ExecutionContext = system.executionContext

    //Create nodes
    for (i <- 0 to 10) {

      var hashID=ChordUtils.md5("N"+i)
      nodeList(i)=hashID
      var actRef=context.spawn(DummyNode(hashID),hashID)
      hashMap.addOne(ChordUtils.md5("N"+i),actRef)
    }
    val r = new scala.util.Random

    //Define and start http server
    val route = path("placeholder") {
      concat(
        get {
          parameters("name".as[String]) { (key) =>
            val index=r.nextInt(10)
            val nodeHash=nodeList(index)
            val x=hashMap.get(nodeHash)
            x.head ! getKeyValue(key)
            complete("Get method done")
          }
        },
        post {
          parameters("name".as[String],"val".as[String]) { (key,value) =>
            val index=r.nextInt(10)
            val nodeHash=nodeList(index)
            val x=hashMap.get(nodeHash)
            x.head ! writeKeyValue(key,value)
            complete("Post method done")
          }

        }
      )
    }
    val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)
    Behaviors.empty
  }

}


object DummyNode {
  var DummyNodeServiceKey = ServiceKey[NodeRequest]("")
  def apply(hash:String): Behavior[NodeRequest] = Behaviors.setup { context =>
    DummyNodeServiceKey=ServiceKey[NodeRequest](hash)
    context.system.receptionist ! Receptionist.Register(DummyNodeServiceKey, context.self)

    Behaviors.receiveMessage {
      case getKeyValue(key) =>
        context.log.info("{} received read request by NODE ACTOR for key: {}", context.self.path.name, key)
        Behaviors.same
      case writeKeyValue(key,value) =>
        context.log.info("{} received write request by NODE ACTOR for key: {}, value: {}", context.self.path.name, key, value)
        Behaviors.same
    }
  }
}
