package monitor.examples.test

import monitor.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    synth.apply("/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/monitor/examples/test", "login.st", synthMonFile = true, synthProtocolFile = true)
  }
}
