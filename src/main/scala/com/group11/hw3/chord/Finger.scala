package com.group11.hw3.chord

import akka.actor.typed.ActorRef
import com.group11.hw3.NodeCommand


case class Finger(start: BigInt, var nodeRef: ActorRef[NodeCommand], var nodeId: BigInt  ) {

}
