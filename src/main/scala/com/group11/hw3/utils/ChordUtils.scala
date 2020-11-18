package com.group11.hw3.utils

import java.math.BigInteger
import java.security.MessageDigest

import com.group11.hw3.NodeConstants.M

object ChordUtils {

  def md5(s: String): BigInt = {
    val digest = new BigInteger(1,MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"))).toString
    BigInt(digest.substring(digest.length-M))
  }
}
