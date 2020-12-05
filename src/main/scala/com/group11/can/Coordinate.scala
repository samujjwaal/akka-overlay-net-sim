package com.group11.can

class Coordinate(var upperX:Double, var upperY:Double, var lowerX:Double, var lowerY:Double) {

def contains( x: Double, y:Double): Boolean =
  {
    if(lowerX <= x && upperX >= x && lowerY <= y && upperY >= y)
      return true;
    false
  }

  def canSplitVertically: Boolean = {
    if (upperX - lowerX >= upperY - lowerY)
      return true
    false
  }

  def isSubsetX(c: Coordinate): Boolean = {
    if ((c.lowerX >= lowerX && c.upperX <= upperX) || (c.lowerX <= lowerX && c.upperX >= upperX)) return true
    false
  }

  def isSubsetY(c: Coordinate): Boolean = {
    if ((c.lowerY >= lowerY && c.upperY <= upperY) || (c.lowerY <= lowerY && c.upperY >= upperY))
      return true
    false
  }

  def isAdjacentX(c: Coordinate): Boolean = {
    if ((c.upperX == lowerX)||(c.lowerX == upperX))
      return true
    false
  }

  def isAdjacentY(c: Coordinate): Boolean = {
    if ((c.lowerY == upperY) || (c.upperY == lowerY))
      return true
    false
  }

  def performSplitVertically(): Unit = {
    upperX = (lowerX + upperX) / 2

  }

  def performSplitHorizontally(): Unit = {
    upperY = (lowerY + upperY) / 2
  }

}
