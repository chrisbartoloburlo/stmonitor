package examples.util

import scala.collection.mutable

object timer {

  private var startTimes= mutable.Stack[Long]()

  def start() = {
    //    startTimes.push(System.currentTimeMillis())
    startTimes.push(System.nanoTime())
  }

  def end(): Long = {
    //    System.currentTimeMillis() - startTimes.pop()
    System.nanoTime() - startTimes.pop()
  }
}