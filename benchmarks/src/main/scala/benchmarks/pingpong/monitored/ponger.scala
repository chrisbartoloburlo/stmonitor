package benchmarks.pingpong.monitored

import lchannels.In

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class ponger(Pinger: In[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
  override def run(): Unit = {
    println("[Ponger] Ponger started, to terminate press CTRL+c")
    var resp = Pinger
    var exit = false
    while(!exit) {
      resp ? {
        case ping @ Ping() =>
//          println("[Ponger] Received Ping()")
//          println("[Ponger] Sending Pong()")
          resp = ping.cont !! Pong()
        case quit @ Quit() =>
          exit = true
      }
    }
  }
}