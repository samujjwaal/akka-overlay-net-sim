package com.group11.hw3

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameters, path, post}
import akka.stream.ActorMaterializer
import com.group11.hw3.utils.Utils
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class HTTPServer {

  val conf: Config = ConfigFactory.load("application.conf")
  val netConf = conf.getConfig("networkConstants")
  implicit val chordSystem: ActorSystem = ActorSystem(netConf.getString("networkSystemName"))
  var bindingFuture: Future[Http.ServerBinding] = _

  def setupServer(chordSystem: ActorSystem, chordNodes: List[BigInt]): Unit = {
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    //Define and start http server
    val route = path("chordRoot") {
      concat(
        get {
          parameters("name".as[String]) { key =>
            val node = Utils.selectRandomNode(chordSystem, chordNodes)
            var msgReply = ""
            def dataRequest(ref:ActorRef[DataResponse]) = GetKeyValue(ref, key)

            context.ask(x.head,dataRequest)
            {
              case Success(DataResponse(message)) => {
                msgReply = message
                AdaptedDataResponse(msgReply)
              }
              case Failure(_) => {
                msgReply = "Could not find Key : "+key
                AdaptedDataResponse(msgReply)
              }
            }
            complete("Read/Get response:"+msgReply)
          }
        },
        post {
          parameters("name".as[String],"val".as[String]) { (key,value) =>
            val index=r.nextInt(NodeConstants.numNodes)
            val nodeHash=nodeList(index)
            val x=hashMap.get(nodeHash)
            x.head ! WriteKeyValue(key,value)
            complete("Post method done")
          }

        }
      )
    }
    val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)
  }

}
