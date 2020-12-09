package com.group11.hw3

import akka.actor.{Actor, ActorLogging, Props}
import scalaj.http._


/**
 * Factory to create Users
 */
object User {

  def props(): Props = Props(new User)
}

class User extends Actor with ActorLogging {

  val request: HttpRequest = Http("http://localhost:9000/chordRoot")

  override def receive: Receive = {
    case CUserReadReq(key) =>
      log.info("{} received read request for key: {}", context.self.path.name, key)
      val readResponse = request.param("name",key).option(HttpOptions.connTimeout(10000)).asString
      log.info("key: {} Read response: {}", key, readResponse.body)
    case CUserWriteReq(key,value) =>
      log.info("{} received write request for key: {}, value: {}", context.self.path.name, key, value)
      val writeResponse = request.params(("name", key), ("val", value)).method("POST").option(HttpOptions.connTimeout(10000)).asString
      log.info("key: {} Write response: {}", key, writeResponse.body)
  }

}


