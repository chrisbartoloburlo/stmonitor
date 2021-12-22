package monitor

import monitor.synth.Synth

import scala.util.Try

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    val preamble = Try(args(2))
    val method = Try(args(3))
    if(method.getOrElse("normal").toLowerCase() != "normal" && method.getOrElse("normal").toLowerCase() != "wilson"){
      println("Error during monitor generation: available approximation methods are 'normal' or 'wilson'")
      return
    }
    synth.apply(args(0), args(1), preamble.getOrElse(""), method.getOrElse("normal"), synthMonFile = true, synthProtocolFile = true)
  }
}