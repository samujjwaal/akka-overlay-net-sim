package com.group11.hw3.utils

import akka.actor.{ActorRef, ActorSystem}

import scala.collection.mutable

object Utils {
  def randomlySelectRequestType(): Boolean = {
    // Implement randomness in selecting request type
    val r = scala.util.Random.nextFloat()
//    println("----r = ",r)
    if (r>0.5) { return true}
    else { return false}
  }

  def randomlySelectDataIndex(maxIndex: Int): Int = {
    // Implement randomness in selecting index of data
//    println("--------------maxIndex",maxIndex)
    return scala.util.Random.nextInt(maxIndex)
  }


  def selectRandomNode(actorSystem: ActorSystem, nodes: mutable.HashMap[BigInt,ActorRef]): ActorRef = {
    val keys=nodes.keys.toList
    val index = scala.util.Random.nextInt(keys.size)

    val key= keys(index)
    nodes.get(key).head
    //actorSystem.actorSelection(nodes(index).toString())

  }
}
