package com.group11.can

class Coordinate(var lowerX:Double, var lowerY:Double, var upperX:Double, var upperY:Double) {

  var centerX: Double = (lowerX + upperX)/2
  var centerY: Double = (lowerY + upperY)/2

  def getAsString() = {
    " lx: "+lowerX.toString + " ly: "+lowerY.toString +" ux: "+upperX.toString +" uy: "+upperY.toString
  }

  def setCoord(l_X: Double, l_Y: Double, u_X:Double, u_Y:Double) = {
    upperX = u_X
    upperY = u_Y
    lowerX = l_X
    lowerY = l_Y
    centerX= (lowerX + upperX)/2
    centerY= (lowerY + upperY)/2
  }
  def canSplitVertically: Boolean = {
    upperX - lowerX >= upperY - lowerY
  }

  def isSubsetX(coordinate_point: Coordinate): Boolean = {
    (coordinate_point.lowerX >= lowerX && coordinate_point.upperX <= upperX) ||
      (coordinate_point.lowerX <= lowerX && coordinate_point.upperX >= upperX)
  }

  def isSubsetY(coordinate_point: Coordinate): Boolean = {
    (coordinate_point.lowerY >= lowerY && coordinate_point.upperY <= upperY) ||
      (coordinate_point.lowerY <= lowerY && coordinate_point.upperY >= upperY)
  }

  def hasPoint(p_x: Double, p_y: Double): Boolean = {
    lowerX<=p_x && p_x<upperX && lowerY<=p_y && p_y<upperY
  }

  def dist(p_x: Double, p_y: Double): Double = {
    Math.sqrt(Math.pow(Math.abs(p_x - centerX),2) + Math.pow(Math.abs(p_y - centerY),2))
  }
  def isAdjacentX(coordinate_point: Coordinate): Boolean = {
    (coordinate_point.upperX == lowerX)||(coordinate_point.lowerX == upperX)
  }

  def isAdjacentY(coordinate_point: Coordinate): Boolean = {
    (coordinate_point.lowerY == upperY) || (coordinate_point.upperY == lowerY)
  }

  def splitVertically(): Unit = {
    upperX = (lowerX + upperX) / 2
    centerX = (lowerX + upperX) / 2
    centerY = (upperY + lowerY) / 2

  }

  def splitHorizontally(): Unit = {
    upperY = (lowerY + upperY) / 2
    centerY = (lowerY + upperY) / 2
    centerX = (lowerX + upperX) / 2
  }

}
