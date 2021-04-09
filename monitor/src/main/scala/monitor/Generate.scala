package monitor

import monitor.synth.Synth

import scala.util.Try

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    val preamble = Try(args(2))
    synth.apply(args(0), args(1), preamble.getOrElse(""), synthMonFile = true, synthProtocolFile = true)
  }
}