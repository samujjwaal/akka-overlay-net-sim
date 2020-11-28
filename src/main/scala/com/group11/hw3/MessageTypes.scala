package com.group11.hw3

import akka.actor.typed.ActorRef
import com.google.gson.JsonObject

trait DataRequest
case class ReadKey(key: String) extends DataRequest
case class WriteValue(key: String, value: String) extends DataRequest

trait NodeCommand
case class NodeAdaptedResponse() extends NodeCommand
case class GetNodeIndex() extends NodeCommand
case class DisplayNodeInfo() extends NodeCommand
//case class FindSuccessor(key: String) extends NodeCommand
case class FindNode(node: ActorRef[Nothing]) extends NodeCommand

case class GetFingerTableStatus(replyTo: ActorRef[FingerTableStatusResponse]) extends NodeCommand
case class FingerTableStatusResponse(ft: String)

case class UpdateFingerTable(nodeRef:ActorRef[NodeCommand],nodeId:BigInt,i:Int,key:BigInt) extends NodeCommand

case class GetKeyValue(replyTo: ActorRef[DataResponse],key: String) extends NodeCommand
case class WriteKeyValue(key: String, value: String) extends  NodeCommand
case class DataResponse(message:String)

case class JoinNetwork(replyTo :ActorRef[JoinStatus],networkRef: ActorRef[NodeCommand]) extends NodeCommand
case class JoinStatus(status: String)

case class FindKeyPredecessor(ref: ActorRef[NodeCommand],key: BigInt) extends NodeCommand
case class FindKeyPredResponse(predId: BigInt, predRef: ActorRef[NodeCommand]) extends NodeCommand

case class FindKeySuccessor(ref: ActorRef[NodeCommand],key: BigInt) extends NodeCommand
case class FindKeySuccResponse(succId: BigInt,succRef: ActorRef[NodeCommand], predId: BigInt, predRef: ActorRef[NodeCommand]) extends NodeCommand

case class GetNodePredecessor(key: BigInt) extends NodeCommand
case class SetNodePredecessor(nodeId: BigInt,nodeRef: ActorRef[NodeCommand]) extends NodeCommand

case class SetNodeSuccessor(nodeId: BigInt,nodeRef: ActorRef[NodeCommand]) extends NodeCommand
case class GetNodeSuccessor(ref: ActorRef[NodeCommand]) extends NodeCommand
case class GetNodeSuccResponse(nodeId: BigInt, nodeRef: ActorRef[NodeCommand]) extends NodeCommand

case class CallFindPredecessor(ref: ActorRef[NodeCommand],key: BigInt) extends NodeCommand
case class CallFindPredResponse(predId: BigInt, predRef: ActorRef[NodeCommand]) extends NodeCommand

case class GetNodeSnapshot(ref:ActorRef[GetNodeSnapshotResponse]) extends  NodeCommand
case class GetNodeSnapshotResponse(snap:JsonObject)



trait ChordSystemCommand
case class UpdateFingerTables() extends ChordSystemCommand
case class WriteInitialData() extends ChordSystemCommand
//case class CaptureGlobalSnapshot() extends ChordSystemCommand

//trait NodeRequest
//case class FindNode(node: ActorRef[Nothing]) extends NodeRequest
//case class getKeyValue(key: String) extends NodeRequest
//case class writeKeyValue(key: String, value: String) extends  NodeRequest
//case class Response(message:String) extends NodeRequest

//case class FindNode(node: ActorRef)