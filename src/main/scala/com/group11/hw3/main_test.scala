package com.group11.hw3

import akka.actor.typed.ActorSystem


/*
Class used to test the basic working of the akka http server.
 */
object main_test {
  def main(args: Array[String]): Unit = {

    val sys=ActorSystem(serverRedone(),"ChordServerSystem")
    Thread.sleep(2000)
//    println("Starting user requests now...")
//    ActorSystem(UserSystem(),"Users")
    sys ! CaptureGlobalSnapshot()

  }

}
