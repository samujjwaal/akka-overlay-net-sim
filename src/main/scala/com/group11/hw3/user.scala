package com.group11.hw3

import akka.NotUsed
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps, Routers}
import akka.actor.typed.{Behavior, SupervisorStrategy}
import scalaj.http._


object User {
  def apply(): Behavior[DataRequest] = Behaviors.setup { context =>
    val request: HttpRequest = Http("http://localhost:9000/placeholder")
    Behaviors.receiveMessage {
      case ReadKey(key) =>
        context.log.info2("{} received read request for key: {}", context.self.path.name, key)
        val readResponse = request.param("name",key).option(HttpOptions.connTimeout(10000)).asString
        context.log.info2("key: {} Read response: {}", key, readResponse.body.toString)
        Behaviors.same
      case WriteValue(key,value) =>
        context.log.info("{} received write request for key: {}, value: {}", context.self.path.name, key, value)
        val writeResponse = request.params(("name", key), ("val", value)).method("POST").option(HttpOptions.connTimeout(10000)).asString
        context.log.info2("key: {} Write response: {}", key, writeResponse.body.toString)
        Behaviors.same
    }
  }
}


object UserSystem{
  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val pool = Routers.pool(poolSize = UserConf.numUsers)(
      Behaviors.supervise(User()).onFailure[Exception](SupervisorStrategy.restart)
    )
    val router = context.spawn(pool,"users-pool")

    router ! ReadKey("readmykey")
    router ! WriteValue("mykey","myvalue")


    Behaviors.empty
  }

}

