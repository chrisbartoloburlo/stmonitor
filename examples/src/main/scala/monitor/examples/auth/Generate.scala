package monitor.examples.auth

import monitor.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    synth.apply(args(0), "auth.st", synthMonFile = true, synthProtocolFile = true)
  }
}
