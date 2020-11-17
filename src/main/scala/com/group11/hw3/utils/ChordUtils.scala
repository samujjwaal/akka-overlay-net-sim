package com.group11.hw3.utils

import java.math.BigInteger
import java.security.MessageDigest

object ChordUtils {

  def md5(s: String): BigInt = new BigInteger(1,MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8")))

}
