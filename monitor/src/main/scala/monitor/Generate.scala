package monitor

import monitor.synth.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    synth.apply(args(0), args(1), synthMonFile = true, synthProtocolFile = true)
  }
}