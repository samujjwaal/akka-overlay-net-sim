package com.group11.hw3.utils

object Utils {
  def randomlySelectRequestType(): Boolean = {
    // Implement randomness in selecting request type
    val r = scala.util.Random.nextFloat()
//    println("----r = ",r)
    if (r>0.5) { return true}
    else { return false}
  }

  def randomlySelectDataIndex(maxIndex: Int): Int = {
    // Implement randomness in selecting index of data
//    println("--------------maxIndex",maxIndex)
    return scala.util.Random.nextInt(maxIndex)
  }
}
