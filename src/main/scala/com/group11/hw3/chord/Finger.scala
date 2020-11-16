package com.group11.hw3.chord

import akka.actor.typed.ActorRef
import com.group11.hw3.NodeCommand


case class Finger(start: Int, node: ActorRef[NodeCommand]  ) {

}
