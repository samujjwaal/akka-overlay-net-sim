package com.group11.can

import akka.actor.ActorRef

class Neighbor(coords: Coordinate, id: BigInt) {
//  val nodeRef: ActorRef = ref
  val nodeCoord: Coordinate = coords
  val nodeId: BigInt = id

  def getAsString(): String = {
    " nodeID : " + nodeId + nodeCoord.getAsString()
  }

}
