package com.group11.hw3

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

class ConfTest extends AnyFunSuite {

  test("Check the parsing of config file"){
    val conf: Config = ConfigFactory.load("application")
    assert(!conf.isEmpty)


    assert(conf.getString("networkConstants.M").toInt==10)
    assert(conf.getString("networkConstants.maxKey").toInt==1024)

  }

}
