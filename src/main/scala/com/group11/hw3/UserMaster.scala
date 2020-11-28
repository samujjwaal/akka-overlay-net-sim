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

  def props(): Props = Props(new UserMaster)
}

class UserMaster extends Actor with ActorLogging {

  val conf: Config = context.system.settings.config

  //Read data from source file
  val dataSource: BufferedSource = Source.fromFile("src/main/resources/listfile.txt")
  val data: List[String] = dataSource.getLines.slice(0,conf.getInt("userConstants.totalRecords")).toList
  val readData: List[String]  = data.slice ( 0, conf.getInt("userConstants.recordsToRead") )
  val writeData: List[String] = data.slice ( conf.getInt("userConstants.recordsToRead"),
                                             conf.getInt("userConstants.totalRecords")
                                            )

  var UserRouter: ActorRef = _

  def createUserRouter(): Unit = {
    log.info("Creating user router.")
    println("Creating user router.")
    UserRouter = context.actorOf(FromConfig.props(User.props()),"user-router")
  }

  override def receive: Receive = {
    case CreateUsers =>
      {
        log.info("Received message create users.")
        println("Received message create users.")
        createUserRouter()
      }

    case StartUserRequests => {
      //Generate and route requests
      var numRequest = 0
      while (numRequest < conf.getInt("userConstants.totalRequest")){
        if (Utils.randomlySelectRequestType()) {

          val index = Utils.randomlySelectDataIndex(readData.size)
          UserRouter ! CReadKey(readData(index).split(',')(0))
        }
        else {
          val index = Utils.randomlySelectDataIndex(writeData.size)
          val record = writeData(index).split(',')
          UserRouter ! CWriteValue(record(0),record(1))
        }
        numRequest = numRequest + 1
      }
    }

    case _ => {
      log.info("Usermaster received a generic message.")
    }
  }

}

