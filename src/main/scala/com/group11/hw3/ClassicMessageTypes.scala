package com.group11.hw3

import akka.actor.ActorRef
import com.google.gson.JsonObject

import scala.collection.mutable


case class CJoinNetwork(shardRegion: ActorRef,peerID: BigInt)
case class CJoinStatus(status: String)

case class CGetNodeNeighbors(key: BigInt)
case class CGetNodeNeighborsResponse(succId: BigInt, predId: BigInt)

case class CSetNodeSuccessor(nodeId: BigInt)
case class CSetNodePredecessor(nodeId: BigInt)

case class CUpdateFingerTable(nodeId:BigInt,i:Int,key:BigInt)

case class CGetFingerTableStatus()
case class CFingerTableStatusResponse(ft: String)

case class CReadKeyValue(key: BigInt, hops: Int)
case class CReadResponse(message:String,hops:Int)
case class CWriteKeyValue(key: BigInt, value: Int,hops: Int)
case class CWriteResponse(hops:Int)
//
//
//case class CGetNodeSnapshot()
//case class CGetNodeSnapshotResponse(snap:JsonObject)


//case class CreateUsers()
//case class StartUserRequests()
case class CUserReadReq(key: String)
case class CUserWriteReq(key: String, value: String)

//case class CCaptureGlobalSnapshot()

final case class EntityEnvelope(id: BigInt, payload: Any)

case class GetStats()
