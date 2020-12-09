package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameters, path, post}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class HTTPServer {

  val conf: Config = ConfigFactory.load("application.conf")
  val netConf = conf.getConfig("networkConstants")

  var bindingFuture: Future[Http.ServerBinding] = _

  def setupServer(chordSystem: ActorSystem, chordShardRegionRef:ActorRef,chordNodes: List[BigInt]): Unit = {
    implicit val chordSystem: ActorSystem = ActorSystem(netConf.getString("networkSystemName"))
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    //Define and start http server
    val route = path("chordRoot") {
      concat(
        get {
          parameters("name".asInstanceOf[String]) { key =>

            //println("Received read request by HTTP server")
            //val node = Utils.selectRandomNode(chordSystem, chordNodes)
            val node = scala.util.Random.nextInt(chordNodes.size)
            var msgReply = ""

            implicit val timeout: Timeout = Timeout(10.seconds)
            //println("--"+node)
            val future = chordShardRegionRef ? EntityEnvelope(node,CGetKeyValue(key.toInt))
            val readValResp = Await.result(future, timeout.duration).asInstanceOf[CDataResponse]
            msgReply=readValResp.message

            complete("Read/Get response:"+msgReply)
          }
        },
        post {
          parameters("name".asInstanceOf[String],"val".asInstanceOf[String]) { (key,value) =>

            //println("Received write request by HTTP server")
            //val node = Utils.selectRandomNode(chordSystem, chordNodes)
            val node=scala.util.Random.nextInt(chordNodes.size)
            chordShardRegionRef ! EntityEnvelope(node , CWriteKeyValue(BigInt(key),value.toInt))

            complete("Post method done")
          }

        }
      )
    }
    val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)
  }

}
