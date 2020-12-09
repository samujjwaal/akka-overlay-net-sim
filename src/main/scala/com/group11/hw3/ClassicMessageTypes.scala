package com.group11.hw3

import akka.actor.ActorRef
import com.google.gson.JsonObject

import scala.collection.mutable


case class CReadKey(key: String)
case class CWriteValue(key: String, value: String)

case class CGetNodeNeighbors(key: BigInt)
case class CGetNodeNeighborsResponse(succId: BigInt, predId: BigInt)

case class CNodeAdaptedResponse()
case class CGetNodeIndex()
case class CDisplayNodeInfo()
//case class FindSuccessor(key: String) extends NodeCommand
case class CFindNode(node: ActorRef)

case class CGetFingerTableStatus()
case class CFingerTableStatusResponse(ft: String)

case class CUpdateFingerTable(nodeId:BigInt,i:Int,key:BigInt)

case class CGetKeyValue(key: Int)
case class CWriteKeyValue(key: String, value: String)
case class CDataResponse(message:String)

case class CJoinNetwork(shardRegion: ActorRef,peerID: BigInt)
case class CJoinStatus(status: String)

case class CFindKeyPredecessor(key: BigInt)
case class CFindKeyPredResponse(predId: BigInt, predRef: ActorRef)

case class CFindKeySuccessor(key: BigInt)
case class CFindKeySuccResponse(succId: BigInt,succRef: ActorRef, predId: BigInt, predRef: ActorRef)

case class CGetNodePredecessor(key: BigInt)
case class CSetNodePredecessor(nodeId: BigInt)

case class CSetNodeSuccessor(nodeId: BigInt)
case class CGetNodeSuccessor()
case class CGetNodeSuccResponse(nodeId: BigInt)

case class CCallFindPredecessor(ref: ActorRef,key: BigInt)
case class CCallFindPredResponse(predId: BigInt, predRef: ActorRef)

case class CGetNodeSnapshot()
case class CGetNodeSnapshotResponse(snap:JsonObject)

case class CFindNodeToWriteData(key: BigInt, value: Int)
case class CWriteDataToNode(key: BigInt, value: Int)
case class CGetValueFromNode(key:Int)

case class CreateNodes()
//case class CaptureGlobalSnapshot()

case class CreateUsers()
case class StartUserRequests()

case class CreateNodesReply(nodeHash:mutable.HashMap[BigInt, ActorRef])

case class CUpdateFingerTables()
case class CWriteInitialData()
case class CCaptureGlobalSnapshot()

final case class EntityEnvelope(id: BigInt, payload: Any)


