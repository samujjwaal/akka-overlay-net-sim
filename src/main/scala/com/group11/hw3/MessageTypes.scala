package com.group11.hw3

import akka.actor.ActorRef

trait DataRequest
case class ReadKey(key: String) extends DataRequest
case class WriteValue(key: String, value: String) extends DataRequest

trait NodeRequest
case class FindNode(node: ActorRef)

trait NodeCommand
case class GetNodeIndex() extends NodeCommand
case class DisplayNodeInfo() extends NodeCommand
case class FindPredecessor(key: String) extends NodeCommand
case class FindSuccessor(key: String) extends NodeCommand