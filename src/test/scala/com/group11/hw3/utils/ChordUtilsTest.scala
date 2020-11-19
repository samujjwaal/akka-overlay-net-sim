package com.group11.hw3.utils

import com.group11.hw3.NodeConstants.M
import com.group11.hw3.utils.ChordUtils.md5
import org.scalatest.funsuite.AnyFunSuite

class ChordUtilsTest extends AnyFunSuite {

  test("test MD5 hashing function") {
    val keyToHash = "DummyKeyValue"
    val expectedHash = md5(keyToHash)
    for(i<- 0 until M)
      assert(expectedHash == md5(keyToHash))
  }

}
