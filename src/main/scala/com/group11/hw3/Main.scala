package com.group11.hw3

import akka.actor.typed.ActorSystem

/*
Class used to test the basic working of the akka http server.
 */
object Main {
  def main(args: Array[String]): Unit = {

    ActorSystem(ChordHttpServer(),"ChordServerSystem")
    Thread.sleep(2000)
    println("Starting user requests now...")
    ActorSystem(UserSystem(),"Users")
//    sys ! CaptureGlobalSnapshot()

  }

}
