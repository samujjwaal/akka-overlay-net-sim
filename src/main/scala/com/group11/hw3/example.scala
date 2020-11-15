package com.group11.hw3

import scala.io.Source

class example {
  val server = new akkaHttpServer
  server.startServer()
  val lines = Source.fromFile("/Users/Al/.bash_profile").getLines.toList
}
