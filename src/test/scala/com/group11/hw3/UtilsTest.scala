package com.group11.hw3

import com.group11.hw3.utils.ChordUtils.md5
import com.group11.hw3.utils.Utils.randomlySelectRequestType
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.control.Breaks.break

class UtilsTest extends AnyFunSuite {

  val conf: Config = ConfigFactory.load("application")
  val M: Int = conf.getInt("networkConstants.M")

  test("Test MD5 hashing function") {
    val keyToHash = "DummyKeyValue"
    val expectedHash = md5(keyToHash)
    for(i<- 0 until M)
      assert(expectedHash == md5(keyToHash))
  }

  test("Test RandomlySelectRequestType()") {
    for(i<- 0 until M) {
      val reqType = randomlySelectRequestType()
      if(reqType) {
        assertThrows[Throwable]{
          break
        }
      }
      else {
        assert(!reqType)
      }
    }
  }
}
