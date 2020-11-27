package com.group11.hw3

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, parameters, path, post}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.group11.hw3.utils.Utils
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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
          parameters("name".asInstanceOf[String]) { key =>
            val node = Utils.selectRandomNode(chordSystem, chordNodes)
            var msgReply = ""

            implicit val timeout: Timeout = Timeout(10.seconds)
            val future = node ? CGetKeyValue(key)
            val readValResp = Await.result(future, timeout.duration).asInstanceOf[CDataResponse]
            msgReply=readValResp.message
//            def dataRequest(ref:ActorRef[DataResponse]) = GetKeyValue(ref, key)
//
//            context.ask(x.head,dataRequest)
//            {
//              case Success(DataResponse(message)) => {
//                msgReply = message
//                AdaptedDataResponse(msgReply)
//              }
//              case Failure(_) => {
//                msgReply = "Could not find Key : "+key
//                AdaptedDataResponse(msgReply)
//              }
//            }
            complete("Read/Get response:"+msgReply)
          }
        },
        post {
          parameters("name".asInstanceOf[String],"val".asInstanceOf[String]) { (key,value) =>

            val node = Utils.selectRandomNode(chordSystem, chordNodes)
            node ! CWriteKeyValue(key,value)
//            val index=r.nextInt(NodeConstants.numNodes)
//            val nodeHash=nodeList(index)
//            val x=hashMap.get(nodeHash)
//            x.head ! WriteKeyValue(key,value)
            complete("Post method done")
          }

        }
      )
    }
    val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)
  }

}
