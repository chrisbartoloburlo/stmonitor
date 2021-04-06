package benchmarks.akkhttppingpong.monitored

import lchannels.{In, LocalChannel}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object Ponger {
  def apply(Pinger: In[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration){
    println("[Ponger] Ponger started, to terminate press CTRL+c")
    var resp = Pinger
    var exit = false
    while (!exit) {
      resp ? {
        case ping@Ping() =>
          //          println("[Ponger] Received Ping()")
          //          println("[Ponger] Sending Pong()")
          resp = ping.cont !! Pong()
        case quit @ Quit() =>
          exit = true
      }
    }
  }
}

object MonitoredPonger extends App {
  def run(): Unit = main(Array())
  val timeout = Duration.Inf

  val (in, out) = LocalChannel.factory[ExternalChoice1]()
  val mon = new Mon(new ConnectionManager(), out, 300)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  Ponger(in)(global, timeout)
}
