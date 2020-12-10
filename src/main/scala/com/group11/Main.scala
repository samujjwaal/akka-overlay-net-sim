package com.group11

import com.group11.can.CANmain
import com.group11.hw3.ChordMain

import java.util.Scanner
import scala.io.StdIn.{readChar, readInt}
import scala.sys.exit

object Main {

  def main(args: Array[String]): Unit = {
    var repeat = 'y'
    do{
      println("\n----- Overlay Network Simulation -----\n")
      println("1. Chord Algorithm")
      println("2. CAN Algorithm")
      println("3. Exit\n")
      print("Enter your choice.. ")
//      val choice = sc.nextInt()
      val choice = readInt()
      if(choice==1)
        ChordMain.execute()
      else if(choice==2)
        CANmain.execute()
      else if(choice == 3)
        exit(0)
      else
        println("Invalid Choice !!")

      print("\nExecute again ? .. ")
      repeat = readChar()
    }
    while (repeat.toLower == 'y')

//    if (args(0) == "1")
//      ChordMain.execute()

  }

}
