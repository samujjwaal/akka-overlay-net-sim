package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameters, path, post}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class HTTPServer {
  val conf: Config = ConfigFactory.load("application.conf")
  val netConf = conf.getConfig("networkConstants")
  var bindingFuture: Future[Http.ServerBinding] = _

  def setupServer(chordSystem: ActorSystem, chordShardRegionRef:ActorRef,chordNodes: List[BigInt]): Route = {
    //Define http server
    val route = path("chordRoot") {
      concat(
        get {
          parameters("name".asInstanceOf[String]) { key =>
            val node = chordNodes(scala.util.Random.nextInt(chordNodes.size))
            implicit val timeout: Timeout = Timeout(10.seconds)
            val future = chordShardRegionRef ? EntityEnvelope(node,CReadKeyValue(key.toInt))
            val readValResp = Await.result(future, timeout.duration).asInstanceOf[CReadResponse]
            complete("Read/Get response: "+readValResp.message)
          }
        },
        post {
          parameters("name".asInstanceOf[String],"val".asInstanceOf[String]) { (key,value) =>
            val node = chordNodes(scala.util.Random.nextInt(chordNodes.size))
            chordShardRegionRef ! EntityEnvelope(node , CWriteKeyValue(BigInt(key),value.toInt))
            complete("Post method done.")
          }
        }
      )
    }
    route

  }

}
