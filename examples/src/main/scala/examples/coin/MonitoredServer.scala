package examples.coin

import lchannels.{In, LocalChannel}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object Server {
  def apply(Client: In[InternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) {
    var quit = false
    var cont = Client
    while (!quit) {
      cont ? {
        case h @ Heads() =>
          println(f"[SRV] Received Heads")
          cont = h.cont
        case t @ Tails() =>
          println(f"[SRV] Received Tails")
          cont = t.cont
      }
    }
  }
}

object MonitoredServer extends App {
  def run() = main(Array())
  val timeout = Duration.Inf

  val (in, out) = LocalChannel.factory[InternalChoice1]()
  val mon = new Mon(new ClientConnectionManager(), out, 300, 4.4172)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  Server(in)(global, timeout)
}
