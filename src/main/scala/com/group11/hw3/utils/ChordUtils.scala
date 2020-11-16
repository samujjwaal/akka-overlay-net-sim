package com.group11.hw3.utils

import java.security.MessageDigest

object ChordUtils {

  def md5(s: String): String = MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8")).map("%02x".format(_)).mkString
  
}
