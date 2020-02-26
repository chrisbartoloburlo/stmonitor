package monitor.examples.game

import monitor.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    synth.apply("/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/monitor/examples/game", "game.st", synthMonFile = true, synthProtocolFile = true)
  }
}
