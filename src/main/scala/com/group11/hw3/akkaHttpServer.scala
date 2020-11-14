package com.group11.hw3

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}

class akkaHttpServer {
  var bindingFuture: Future[Http.ServerBinding] = _
  def startServer(): Unit =
  {
    implicit val system: ActorSystem = ActorSystem("helloworld")
    implicit val executor: ExecutionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val route = path("placeholder") {
      concat(
        get {    complete("Hello, World!")  },
        post {   complete("Post method")    }

      )

    }

    bindingFuture=Http().bindAndHandle(route, "localhost", 9000)
  }

}
