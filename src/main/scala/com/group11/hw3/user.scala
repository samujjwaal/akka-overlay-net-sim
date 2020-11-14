package com.group11.hw3

import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
//import akka.http.scaladsl.client.RequestBuilding._
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//import scala.concurrent.Future
import scalaj.http._

//trait DataRequest
//case class ReadKey(key: String) extends DataRequest
//case class WriteValue(key: String, value: String) extends DataRequest

object User {
  def apply(): Behavior[DataRequest] =
    Behaviors.setup(context => new User(context))
}

class User(context: ActorContext[DataRequest]) extends AbstractBehavior[DataRequest](context) {

  val request: HttpRequest = Http("http://localhost:8080/application")
  override def onMessage(msg: DataRequest): Behavior[DataRequest] =
    msg match {
      case ReadKey(key) =>
        context.log.info("{} received message: {}", context.self.path.name)
        val readResponse = request.param("name",key).asString
        this
      case WriteValue(key,value) =>
        val writeResponse = request.postForm(Seq("name"->key,"val"->value)).asString
        this
    }
}