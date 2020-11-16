package com.group11.hw3

import akka.NotUsed
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, path, post}

import scala.concurrent.ExecutionContext

object serverRedone {

  def apply(): Behavior[Nothing] = Behaviors.setup { context =>

    //Start the server
    val pool = Routers.pool(poolSize = ServerConf.numOfNodes)(
      Behaviors.supervise(DummyNode()).onFailure[Exception](SupervisorStrategy.restart)
    )
    val chordRouter = context.spawn(pool,"users-pool")
    implicit val system:ActorSystem[NotUsed]=ActorSystem(Behaviors.empty,"http-server")
    implicit val executor: ExecutionContext = system.executionContext

    val route = path("placeholder") {
      concat(
        get {
          //context.ask(chordRouter,getKeyValue(context.self,"key"))
          chordRouter ! getKeyValue(context.self,"key")
          complete("Get method")

        },
        post {

          complete("Post method")
        }
      )
    }
    val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)
    
    Behaviors.empty
  }

}


object DummyNode {
  def apply(): Behavior[NodeRequest] = Behaviors.setup { context =>
    //val request: HttpRequest = Http("http://localhost:9000/placeholder")
    Behaviors.receiveMessage {
      case ReadKey(key) =>
        context.log.info("{} received read request for key: {}", context.self.path.name, key)
        Behaviors.same
      case WriteValue(key,value) =>
        context.log.info("{} received write request for key: {}, value: {}", context.self.path.name, key, value)
        Behaviors.same
    }
  }
}
