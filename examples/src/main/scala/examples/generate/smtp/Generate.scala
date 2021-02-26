package examples.generate.smtp

import monitor.synth.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
//    args(0)
    synth.apply("/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/examples/generate/smtp", "smtp.st", synthMonFile = true, synthProtocolFile = true)
  }
}