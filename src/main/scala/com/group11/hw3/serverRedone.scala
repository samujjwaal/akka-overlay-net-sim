package com.group11.hw3

import akka.NotUsed
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, path, post, _}
import akka.util.Timeout
import com.group11.hw3.utils.ChordUtils
import com.group11.hw3.chord.{ChordNode, Finger}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object serverRedone {
  //private case class AdaptedResponse(message: String) extends ChordSystemCommand
  def apply(): Behavior[ChordSystemCommand] = Behaviors.setup { context =>
    var hashMap = new mutable.HashMap[BigInt,ActorRef[NodeCommand]]()
    val nodeList = new ListBuffer[BigInt]()

    implicit val system:ActorSystem[NotUsed]=ActorSystem(Behaviors.empty,"http-server")
    implicit val executor: ExecutionContext = system.executionContext

    //Create nodes
    while (nodeList.size < NodeConstants.numNodes) {

      var hashID=ChordUtils.md5("N"+nodeList.size)
      if (!(nodeList.contains(hashID))) {
        nodeList += hashID
        var actRef = context.spawn(ChordNode(hashID), hashID.toString())
        hashMap.addOne(hashID, actRef)
      }
    }
    val r = new scala.util.Random
    implicit val timeout: Timeout = 3.seconds

    //Define and start http server
    val route = path("placeholder") {
      concat(
        get {
          parameters("name".as[String]) { (key) =>
            val index=r.nextInt(NodeConstants.numNodes)
            val nodeHash=nodeList(index)
            val x=hashMap.get(nodeHash)
            var msgReply = ""
            def buildRequest(ref:ActorRef[NodeCommand]) =
              getKeyValue(ref, key)

            val ref = x.head
//            x.head ! buildRequest
            context.ask(x.head,buildRequest)
            {
              case Success(Response(message)) => {
                msgReply = message
                AdaptedResponse(msgReply)
              }
              case Failure(_) => {
                msgReply = "Could not find Key : "+key
                AdaptedResponse(msgReply)
              }
            }
            complete("Read/Get response:"+msgReply)
          }
        },
        post {
          parameters("name".as[String],"val".as[String]) { (key,value) =>
            val index=r.nextInt(10)
            val nodeHash=nodeList(index)
            val x=hashMap.get(nodeHash)
            x.head ! writeKeyValue(key,value)
            complete("Post method done")
          }

        }
      )
    }
    val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)

    Behaviors.receiveMessage[ChordSystemCommand] {

      case UpdateFingerTables() =>
        Behaviors.same

      case WriteInitialData() =>
        Behaviors.same
    }
    Behaviors.empty
  }



}


//object DummyNode {
////  var DummyNodeServiceKey = ServiceKey[NodeRequest]("")
////  trait NodeRequest
////  case class FindNode(node: ActorRef[Nothing]) extends NodeRequest
////  case class getKeyValue(key: String,actorRef: ActorRef[Response]) extends NodeRequest
////  case class writeKeyValue(key: String, value: String) extends  NodeRequest
////  case class Response(message:String) extends NodeRequest
//
//  def apply(hash:BigInt): Behavior[NodeCommand] = Behaviors.setup { context =>
////    DummyNodeServiceKey=ServiceKey[NodeRequest](hash.toString())
////    context.system.receptionist ! Receptionist.Register(DummyNodeServiceKey, context.self)
//
//    Behaviors.receiveMessage {
//      case getKeyValue(key) =>
//        context.log.info("{} received read request by NODE ACTOR for key: {}", context.self.path.name, key)
//        //replyTo ! Response("Dummy value!")
//        Behaviors.same
//      case writeKeyValue(key,value) =>
//        context.log.info("{} received write request by NODE ACTOR for key: {}, value: {}", context.self.path.name, key, value)
//        Behaviors.same
//    }
//  }
//}
