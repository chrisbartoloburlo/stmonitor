package examples.generate.coin

import monitor.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    synth.apply(args(0), "coin.st", synthMonFile = true, synthProtocolFile = true)
  }
}
