package com.group11.can

import com.group11.can.CanMessageTypes._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import com.typesafe.config.Config

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

object CanNode {
  def props(id:BigInt):Props= {
    Props(new CanNode(id:BigInt))
  }

}

class CanNode(myId:BigInt) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher

  val nodeConf: Config = context.system.settings.config.getConfig("CANnetworkConstants")
  val xMax = nodeConf.getInt("xMax")
  val yMax = nodeConf.getInt("yMax")
  var myCoord: Coordinate = null
  val myNeighbors = new ListBuffer[Neighbor]()

  def splitMyZone(newNode: ActorRef): Unit = {

  }

  def findClosestNeighbor(p_x: Double, p_y: Double): Neighbor = {
    var closestNbr: Neighbor = null
    var dist = Double.MaxValue
    for (nbr <- myNeighbors) {
      val nbrDist = nbr.nodeCoord.dist(p_x,p_y)
      if (nbrDist < dist) {
        dist = nbrDist
        closestNbr = nbr
      }
    }
    closestNbr
  }

  override def receive: Receive = {
    case JoinCan(peer: ActorRef) => {
      if (peer == self) {
        myCoord = new Coordinate(xMax,yMax,0,0)
      }
      else {
        val p_x = scala.util.Random.nextDouble() * xMax
        val p_y = scala.util.Random.nextDouble() * yMax
        peer ! RouteNewNode(p_x, p_y, self)
      }
    }

    case RouteNewNode(p_x, p_y, newNode) => {
      if (myCoord.hasPoint(p_x,p_y)) {
        splitMyZone(newNode)
      }
      else {
        val closestNeighbor = findClosestNeighbor(p_x,p_y)
        closestNeighbor.nodeRef ! RouteNewNode(p_x, p_y, newNode)
      }
    }

    case GetNodeId() => {
      sender() ! myId
    }

    case GetNeighbors() => {
      sender() ! myNeighbors
    }

    case GetCoord() => {
      sender() ! myCoord
    }

    case SetCoord(l_X: Double, l_Y: Double, u_X:Double, u_Y:Double) => {
      myCoord.setCoord(l_X, l_Y, u_X, u_Y)
    }
  }
}
