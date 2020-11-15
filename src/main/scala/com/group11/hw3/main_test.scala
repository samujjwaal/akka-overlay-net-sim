package com.group11.hw3

import scala.io.Source


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
  }

}
