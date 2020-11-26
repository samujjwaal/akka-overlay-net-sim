package com.group11.hw3

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.FromConfig
import com.group11.hw3.utils.Utils
import com.typesafe.config.Config

import scala.io.{BufferedSource, Source}

/**
 * Factory to create the User Actor System
 */
object UserMaster {
  case class CreateUsers()
  case class StartUserRequests()
  def props(): Props = Props(new UserMaster)
}

class UserMaster extends Actor with ActorLogging {
  import UserMaster._

  val userConf: Config = context.system.settings.config

  //Read data from source file
  val dataSource: BufferedSource = Source.fromFile("src/main/resources/listfile.txt")
  val data: List[String] = dataSource.getLines.slice(0,userConf.getInt("userConstants.totalRecords")).toList
  val readData: List[String]  = data.slice ( 0, userConf.getInt("userConstants.recordsToRead") )
  val writeData: List[String] = data.slice ( userConf.getInt("userConstants.recordsToRead"),
                                             userConf.getInt("userConstants.totalRecords")
                                            )

  var UserRouter: ActorRef = _

  def createUserRouter(): Unit = {
    UserRouter = context.actorOf(FromConfig.props(User.props()),"user-router")
  }

  override def receive: Receive = {
    case CreateUsers() => createUserRouter()

    case StartUserRequests() => {
      //Generate and route requests
      var numRequest = 0
      while (numRequest < userConf.getInt("userConstants.totalRequest")){
        if (Utils.randomlySelectRequestType()) {

          val index = Utils.randomlySelectDataIndex(readData.size)
          UserRouter ! ReadKey(readData(index).split(',')(0))
        }
        else {
          val index = Utils.randomlySelectDataIndex(writeData.size)
          val record = writeData(index).split(',')
          UserRouter ! WriteValue(record(0),record(1))
        }
        numRequest = numRequest + 1
      }
    }
  }

}

