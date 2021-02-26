package examples.generate.auth

import monitor.synth.Synth

// sbt "project examples" "runMain monitor.examples.testauth.Generate /Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/monitor/generate/auth"

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
    synth.apply(args(0), "auth.st", synthMonFile = true, synthProtocolFile = true)
  }
}
