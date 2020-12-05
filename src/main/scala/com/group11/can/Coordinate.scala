package com.group11.can

class Coordinate(lX:Double, lY:Double, uX:Double, uY:Double) {
  var upperX: Double = lX
  var upperY: Double = lY
  var lowerX: Double = uX
  var lowerY: Double = uY
  var centerX: Double = (lowerX + upperX)/2
  var centerY: Double = (lowerY + upperY)/2

  def setCoord(l_X: Double, l_Y: Double, u_X:Double, u_Y:Double) = {
    upperX = u_X
    upperY = u_Y
    lowerX = l_X
    lowerY = l_Y
    centerX= (lowerX + upperX)/2
    centerY= (lowerY + upperY)/2
  }
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

  def hasPoint(p_x: Double, p_y: Double): Boolean = {
    lowerX<=p_x && p_x<upperX && lowerY<=p_y && p_y<upperY
  }

  def dist(p_x: Double, p_y: Double): Double = {
    Math.sqrt(Math.pow(Math.abs(p_x - centerX),2) + Math.pow(Math.abs(p_y - centerY),2))
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
