package com.group11.hw3

import akka.actor.ActorRef

trait DataRequest
case class ReadKey(key: String) extends DataRequest
case class WriteValue(key: String, value: String) extends DataRequest

trait NodeRequest
case class FindNode(node: ActorRef)