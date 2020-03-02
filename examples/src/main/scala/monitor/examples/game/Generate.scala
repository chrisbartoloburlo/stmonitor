package monitor.examples.game

import monitor.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    synth.apply(args(0), "game.st", synthMonFile = true, synthProtocolFile = true)
  }
}
