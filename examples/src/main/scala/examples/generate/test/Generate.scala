package examples.generate.test

import monitor.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
//    args(0)
    synth.apply("/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/examples/generate/test", "test.st", synthMonFile = false, synthProtocolFile = false)
  }
}