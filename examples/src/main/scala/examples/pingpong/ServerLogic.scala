package examples.pingpong

import scala.concurrent.duration.Duration

import lchannels.In

class ServerLogic(pinger: In[InternalChoice1])(implicit timeout: Duration) extends Runnable {
  override def run(): Unit = {
    var resp = pinger
    var exit = false
    while (!exit) {
      resp ? {
        case ping @ Ping() =>
          resp = ping.cont !! Pong()
        case quit @ Quit() =>
          exit = true
      }
    }
  }
}
