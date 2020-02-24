package monitor.examples.login

import monitor.interpreter.STInterpreter
import monitor.parser.STParser

import scala.io.Source

object GenMon {
  def main(args: Array[String]) {
    val path = "/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/monitor/examples/login"
    val inputFile = Source.fromFile(path+"/login.st")
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

      case parser.Error(msg, n) => println("Error: " + msg + " n: "+n.offset )

      case parser.Failure(msg, n) => println("Error: " + msg + " n: "+n.offset )

      case _ =>

    }
    inputFile.close()
  }
}
