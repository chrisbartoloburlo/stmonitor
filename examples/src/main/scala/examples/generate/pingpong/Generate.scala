package examples.generate.pingpong

import monitor.synth.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
//    args(0)
    synth.apply("/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/examples/generate/pingpong", "ponger.st", synthMonFile = true, synthProtocolFile = true)
  }
}