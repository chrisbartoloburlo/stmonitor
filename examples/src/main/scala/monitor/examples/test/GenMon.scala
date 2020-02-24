package monitor.examples.test

import monitor.interpreter.STInterpreter
import monitor.parser.STParser

import scala.io.Source

object GenMon {
  def main(args: Array[String]) {
    val path = "/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/monitor/examples/test"
    val inputFile = Source.fromFile(path+"/test.st")
    val inputSource = inputFile.mkString

    val parser = new STParser

    parser.parseAll(parser.sessionTypeVar, inputSource) match {

      case parser.Success(r, n) =>

        println("parser success")

        val interpreter = new STInterpreter(r, parser.getGlobalVar, path)
        try {
          interpreter.run()
        } catch {
          case e: RuntimeException => println(e.getMessage)
        }

      case parser.Error(msg, n) => println("Error: " + msg)

      case parser.Failure(msg, n) => println("Error: " + msg)

      case _ =>

    }
    inputFile.close()
  }
}
