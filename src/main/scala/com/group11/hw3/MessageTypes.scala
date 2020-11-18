package com.group11.hw3

import akka.actor.typed.ActorRef

trait DataRequest
case class ReadKey(key: String) extends DataRequest
case class WriteValue(key: String, value: String) extends DataRequest

//trait NodeRequest
//case class FindNode(node: ActorRef[Nothing]) extends NodeRequest
//case class getKeyValue(key: String) extends NodeRequest
//case class writeKeyValue(key: String, value: String) extends  NodeRequest
//case class Response(message:String) extends NodeRequest




//case class FindNode(node: ActorRef)

trait NodeCommand
case class NodeAdaptedResponse() extends NodeCommand
case class GetNodeIndex() extends NodeCommand
case class DisplayNodeInfo() extends NodeCommand
//case class FindSuccessor(key: String) extends NodeCommand
case class FindNode(node: ActorRef[Nothing]) extends NodeCommand
case class UpdateFingerTable() extends NodeCommand
case class getKeyValue(node:ActorRef[NodeCommand],key: String) extends NodeCommand
case class writeKeyValue(key: String, value: String) extends  NodeCommand
case class HttpResponse(message:String) extends NodeCommand

case class JoinNetwork(master:ActorRef[NodeCommand],networkRef: ActorRef[NodeCommand]) extends NodeCommand
case class JoinStatus(status: String) extends NodeCommand

case class FindKeyPredecessor(ref: ActorRef[NodeCommand],key: BigInt) extends NodeCommand
case class FindKeyPredResponse(predId: BigInt, predRef: ActorRef[NodeCommand]) extends NodeCommand

case class FindKeySuccessor(ref: ActorRef[NodeCommand],key: BigInt) extends NodeCommand
case class FindKeySuccResponse(succId: BigInt,succRef: ActorRef[NodeCommand], predId: BigInt, predRef: ActorRef[NodeCommand]) extends NodeCommand

case class FindNodePredecessor(key: BigInt) extends NodeCommand
case class SetNodePredecessor(nodeId: BigInt,nodeRef: ActorRef[NodeCommand]) extends NodeCommand

case class SetNodeSuccessor(nodeId: BigInt,nodeRef: ActorRef[NodeCommand]) extends NodeCommand
case class GetNodeSuccessor(ref: ActorRef[NodeCommand]) extends NodeCommand
case class GetNodeSuccResponse(nodeId: BigInt, nodeRef: ActorRef[NodeCommand]) extends NodeCommand

case class CallFindPredecessor(ref: ActorRef[NodeCommand],key: BigInt) extends NodeCommand
case class CallFindPredResponse(predId: BigInt, predRef: ActorRef[NodeCommand]) extends NodeCommand


trait ChordSystemCommand
case class UpdateFingerTables() extends ChordSystemCommand
case class WriteInitialData() extends ChordSystemCommand
case class AdaptedResponse(msg: String) extends ChordSystemCommand