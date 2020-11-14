package com.group11.hw3


/*
Class used to test the basic working of the akka http server.
 */
object main_test {
  def main(args: Array[String]): Unit = {
    val serv= new akkaHttpServer
    serv.startServer()
  }

}
