package com.group11

import com.group11.can.CANmain
import com.group11.hw3.ChordMain
import org.slf4j.LoggerFactory

import scala.io.StdIn.{readChar, readInt}
import scala.sys.exit

object Main {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    var repeat = 'y'
    do{
      println("\n----- Overlay Network Simulation -----\n")
      println("1. Chord Algorithm")
      println("2. CAN Algorithm\n")
      print("Enter your choice.. ")
      val choice = readInt()
      if(choice==1) {
        logger.info(" -- Simulating Chord Algorithm -- \n")
        ChordMain.execute()
      } else if(choice==2) {
        logger.info(" -- Simulating CAN Algorithm -- \n")
        CANmain.execute()
      }
      else
        println("Invalid Choice !!")

      print("\nExecute again ? .. ")
      repeat = readChar()
    }
    while (repeat.toLower == 'y')

    logger.info("\nExiting Simulation")
    logger.info("\nDetailed logs available in project.log")
  }

}
