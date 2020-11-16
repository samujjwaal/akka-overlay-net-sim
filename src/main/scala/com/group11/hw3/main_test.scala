package com.group11.hw3

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors

import scala.io.Source
import akka.actor.typed.{ActorSystem, Behavior}


/*
Class used to test the basic working of the akka http server.
 */
object main_test {
  def main(args: Array[String]): Unit = {
    val serv= new akkaHttpServer
    serv.startServer()
    val lines = Source.fromFile("src/main/resources/listfile.txt").getLines.slice(0,2000).toList
    println(lines.head)
    println(lines.size)

    // Create and setup the Chord server and actor system before creating users and sending in requests to the server
    val chordSystem = ActorSystem(ChordSystem(),"ChordServerSystem")

    // Create User actor system which sends read/write requests to the Chord server
    val userSystem  = ActorSystem(UserSystem(),"Users")

  }

}

object ChordSystem {
  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    Behaviors.empty
  }
}