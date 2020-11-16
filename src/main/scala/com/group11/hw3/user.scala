package com.group11.hw3

import akka.NotUsed
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import scalaj.http._
import scala.io.Source


object User {
  def apply(): Behavior[DataRequest] = Behaviors.setup { context =>
    val request: HttpRequest = Http("http://localhost:9000/placeholder")
    Behaviors.receiveMessage {
      case ReadKey(key) =>
        context.log.info2("{} received read request for key: {}", context.self.path.name, key)
        val readResponse = request.param("name",key).asString
        context.log.info2("key: {} Read response: {}", key, readResponse.body.toString)
        Behaviors.same
      case WriteValue(key,value) =>
        context.log.info("{} received write request for key: {}, value: {}", context.self.path.name, key, value)
        val writeResponse = request.postForm(Seq("name"->key,"val"->value)).asString
        context.log.debug("key: {} Write response: {}", key, writeResponse.body.toString)
        Behaviors.same
      case _ =>
        Behaviors.unhandled
    }
  }
}


object UserSystem{
  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val pool = Routers.pool(poolSize = UserConf.numUsers)(
      Behaviors.supervise(User()).onFailure[Exception](SupervisorStrategy.restart)
    )
    val router = context.spawn(pool,"users-pool")

    val dataSource = Source.fromFile("src/main/resources/listfile.txt")
    val data = dataSource.getLines.slice(0,UserConf.totalRecords).toList
    val readData: List[String]  = data.slice(0,UserConf.recordsToRead)
    val writeData: List[String] = data.slice(UserConf.recordsToRead,UserConf.totalRecords)

    var numRequest = 0
    while (numRequest < UserConf.totalRequest){
      if (Utils.randomlySelectRequestType()) {
//        println("------ readData size : ",readData.size)
        val index = Utils.randomlySelectDataIndex(readData.size)
        router ! ReadKey(readData(index).split(',')(0))
      }
      else {
//        println("------ writeData size : ",writeData.size)
        val index = Utils.randomlySelectDataIndex(writeData.size)
        val record = writeData(index).split(',')
        router ! WriteValue(record(0),record(1))
      }
      numRequest = numRequest + 1
    }


    Behaviors.empty
  }

}

