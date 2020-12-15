package monitor.generate.smtp

import monitor.Synth

object Generate {
  def main(args: Array[String]): Unit = {
    val synth = new Synth()
//    args(0)
    synth.apply("/Users/Chris/Documents/MSc/stmonitor/examples/src/main/scala/monitor/generate/smtp", "smtp.st", synthMonFile = false, synthProtocolFile = true)
  }
}