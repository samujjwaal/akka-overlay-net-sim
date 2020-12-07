package com.group11.can

import akka.actor.{ActorRef}

object CanMessageTypes {
  case class JoinCan(existingNode: ActorRef)
  case class RouteNewNode(p_x: Double, p_y: Double, newNode: ActorRef)

  case class GetNodeId()
  case class GetNeighbors()
  case class GetCoord()
  case class SetCoord(l_x:Double,l_y:Double,u_x:Double,u_y:Double)

  case class AddNeighbor(nbrRef: ActorRef,lx:Double,ly:Double,ux:Double,uy:Double, nbrID: BigInt)
  case class RemoveNeighbor(nbrID: BigInt)
  case class UpdateNeighbor(nbrID: BigInt,lx:Double,ly:Double,ux:Double,uy:Double)

  case class RoutingDone(message: String)
}
