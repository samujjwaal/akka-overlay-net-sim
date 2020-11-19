package com.group11.hw3

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

class ConfTest extends AnyFunSuite {

  test("Check the parsing of config file"){
    val conf: Config = ConfigFactory.load("TestConfig")
    assert(!conf.isEmpty)


    assert(conf.getString("numUsers").toInt==4)
    assert(conf.getString("totalRecords").toInt==5000)

  }

}
