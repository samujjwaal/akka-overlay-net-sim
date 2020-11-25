package com.group11.hw3.chord

import akka.actor.ActorRef


case class ClassicFinger(var start: BigInt, var nodeRef: ActorRef, var nodeId: BigInt  ) {

}
