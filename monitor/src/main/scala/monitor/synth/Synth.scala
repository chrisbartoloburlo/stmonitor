package monitor.synth

import com.typesafe.scalalogging.Logger
import monitor.interpreter.STInterpreter
import monitor.parser.STParser

import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Try

class Synth {
  /**
   * Synthesises the monitor and the protocol files.
   *
   * @param directoryPath The path containing the util.scala file which also represents the directory where the monitor
   *             and protocol files are to be generated in.
   * @param sessionTypePath The path of the file containing the session type.
   * @param preamblePath The path to the file containing the preamble of the monitor code.
   * @param synthMonFile A flag to indicate whether to synthesise the monitor file or not.
   * @param synthProtocolFile A flag to indicate whether to synthesise the protocol file or not.
   */
  def apply(directoryPath: String, sessionTypePath: String, preamblePath: String, method: String, synthMonFile: Boolean, synthProtocolFile: Boolean): Unit ={
    val logger = Logger("Synth")
    val inputFile = Source.fromFile(sessionTypePath)
    val inputSource = inputFile.mkString

    val parser = new STParser
    parser.parseAll(parser.sessionTypeVar, inputSource) match {
      case parser.Success(r, n) =>
        val stFile = sessionTypePath.substring(sessionTypePath.lastIndexOf('/')+1)
        logger.info(f"Input type $stFile parsed successfully")
        val preambleFile = Try(Source.fromFile(preamblePath).mkString)
        val interpreter = new STInterpreter(r, directoryPath, preambleFile.getOrElse(""), method)
        try {
          val (mon, protocol) = interpreter.run()
          if(synthMonFile){
            lazy val monFile = new PrintWriter(new File(directoryPath+"/Monitor.scala"))
            monFile.write(mon.toString)
            monFile.close()
          }
          if(synthProtocolFile){
            lazy val protocolFile = new PrintWriter(new File(directoryPath+"/CPSPc.scala"))
            protocolFile.write(protocol.toString)
            protocolFile.close()
          }
        } catch {
          case e: Exception =>
            println("Error: " + e.getMessage)
        }

      case parser.Error(msg, n) =>
        println("Parser Error: " + msg + " offset: "+n.offset )

      case parser.Failure(msg, n) =>
        println("Parser Error: " + msg + " offset: "+n.offset )

      case _ =>

    }
    inputFile.close()
  }
}
