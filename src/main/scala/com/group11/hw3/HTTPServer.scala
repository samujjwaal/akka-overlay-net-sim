package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameters, path, post}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Success

class HTTPServer {
  val conf: Config = ConfigFactory.load("application.conf")
  val netConf = conf.getConfig("networkConstants")
  var hopsPerReq = 0

  def setupServer(chordSystem: ActorSystem, chordShardRegionRef:ActorRef,chordNodes: List[BigInt]): Route = {
    //Define http server
    implicit val ec: ExecutionContext = chordSystem.dispatcher
    val route = path("chordRoot") {
      concat(
        get {
          parameters("name".asInstanceOf[String]) { key =>
            val node = chordNodes(scala.util.Random.nextInt(chordNodes.size))
            var readValResp = ""
            implicit val timeout: Timeout = Timeout(10.seconds)
//            val future = chordShardRegionRef ? EntityEnvelope(node,CReadKeyValue(key.toInt))
//            val readValResp = Await.result(future, timeout.duration).asInstanceOf[CReadResponse]
            val future = (chordShardRegionRef ? EntityEnvelope(node , CReadKeyValue(key.toInt,0))).mapTo[CReadResponse]
            future.onComplete( {
              case Success(resp) => {
                readValResp = resp.message
                hopsPerReq += resp.hops
              }
            })
            complete("Read/Get response: "+readValResp)
          }
        },
        post {
          parameters("name".asInstanceOf[String],"val".asInstanceOf[String]) { (key,value) =>
            val node = chordNodes(scala.util.Random.nextInt(chordNodes.size))
            implicit val timeout: Timeout = Timeout(10.seconds)
            val future = (chordShardRegionRef ? EntityEnvelope(node , CWriteKeyValue(BigInt(key),value.toInt,0))).mapTo[CWriteResponse]
            future.onComplete( {
              case Success(resp) => {
                hopsPerReq += resp.hops
              }
            })
            complete("Post method done.")
          }
        }
      )
    }
    route

  }

//  def stopServer(): Unit =
//  {
//    implicit val execContext:ExecutionContextExecutor=chordActorSystem.dispatcher
//      bindingFuture.flatMap(_.unbind())
//  }

}
