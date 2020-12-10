package com.group11.hw3

import com.group11.can.Coordinate
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

class CANUtilsTest extends AnyFunSuite {
  val conf: Config = ConfigFactory.load("application")
  val xMax: Double = conf.getDouble("CANnetworkConstants.xMax")
  val yMax: Double = conf.getDouble("CANnetworkConstants.yMax")

  test("Test CAN coordinate Zone creation") {
    val zone = new Coordinate(0, 0, xMax, yMax)
    println(zone.getAsString())
  }

  test("Test CAN coordinate Zone splitting") {
    val zone = new Coordinate(0, 0, xMax, yMax)
    assert(zone.canSplitVertically)
    zone.splitVertically()
    assert(zone.upperX==xMax/2)
    assert(zone.centerX==zone.upperX/2)
    assert(zone.centerY==yMax/2)

    zone.setCoord(2,3,5,7)
    println(zone.getAsString())
    assert(!zone.canSplitVertically)

  }

  test("Test coordinate zone adjacency"){
    val zone1 = new Coordinate(5, 6, 10, 12)
    val zone2 = new Coordinate(5, 7, 5, 6)
    assert(zone1.isAdjacentX(zone2))
    val zone3 = new Coordinate(10, 12, 2, 3)
    assert(zone1.isAdjacentY(zone3))
  }

  test("Test point lies in a coordinate zone"){
    val zone = new Coordinate(5, 6, 10, 12)
    assert(!zone.hasPoint(10,10))
    assert(zone.hasPoint(8,8))
    assert(zone.dist(7,7)>0)
  }

  test("Test coordinate zone is subset of a zone"){
    val zone1 = new Coordinate(0, 0, xMax, yMax)
    val zone2 = new Coordinate(5, 7, 5, 6)
    assert(zone1.isSubsetX(zone2))
    assert(zone1.isSubsetY(zone2))
  }
}