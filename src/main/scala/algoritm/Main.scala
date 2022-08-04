package algoritm

import scala.annotation.tailrec
import scala.util.Sorting

object Main {
  def main(array: Array[String]): Unit = {
      println(s"""
       |${canSum(0, Array(9, 1, 2, 3))} // false
       |${canSum(1, Array(8, 1, 2, 3))} // true
       |${canSum(7, Array(3, 2, 1, 4))} // true
       |${canSum(2, Array(5, 1, 2, 3))} // true
       |${canSum(20, Array(67, 11, 22, 3, 9))} // true
       |${canSum(19, Array(67, 11, 22, 3, 9))} // false
       |${canSum(300, Array(7, 11))} // false
       |
       |""".stripMargin)
  }

  // 7, Array(1, 2, 3, 4, 5)
  /*
  * Write a function canSum(targetSum, numbers) that takes in a targetSum and an array of numbers as arguments.
  * The function should return boolean -> if it's can sum all numbers to fetch target sum or not
  *
  * */

  def canSum(targetSum: Int, numbers: Array[Int]): Boolean = {
    




    if (targetSum == 0) true
    else if (targetSum < 0) false
    else {
      (for {
        el <- numbers
        stepResult = targetSum - el
      } yield canSum(stepResult, numbers)).contains(true)
    }
  }



  /*

  Say that you are a traveler on a 2D gridd. You begin in the top-lef corner and your goal is to travel
  to the bottom-right corner. You may move only down or right.

  Find all possible ways
  added mutable variable which allow us to hold already know stuff - so we do not need to go one more time

  println(travelGrid(1,1)) // 1
  println(travelGrid(2,3)) // 3
  println(travelGrid(3,2)) // 3
  println(travelGrid(3,3)) // 6
  println(travelGrid(12,12)) // 6
  println(travelGrid(18,18)) // 2333606220

  with a mutable map complexity is a O(m*n)
  without a mutable map it's about O(2^m+n)
 */
  def travelGrid(row: Long, col: Long): Long = {
    val memo = scala.collection.mutable.Map.empty[(Long, Long), Long]

    def inner(rowI: Long, colIn: Long): Long = {
      (rowI, colIn) match {
        case (r, c) if memo.contains((r, c)) => memo((r, c))
        case (r, c) if r == 0 || c == 0      => 0
        case (r, c) if r == 1 && c == 1      => 1
        case (r, c)                          =>
          memo.addOne((r, c) -> (inner(r - 1, c) + inner(r, c - 1)))
          memo.apply((r, c))
      }
    }
    inner(row, col)
  }

  /*   fibonachi */

  // 5 fib -> 3
  // 0 1 1 2 3 5 8 13 21
  def fibonachi(n: Long): Long = {
    @tailrec
    def inner(nInner: Long, stepResult: Long, result: Long): Long = nInner match {
      case 0 => stepResult
      case _ => inner(nInner - 1, result, result + stepResult)
    }
    inner(n, 0, 1)
  }

}