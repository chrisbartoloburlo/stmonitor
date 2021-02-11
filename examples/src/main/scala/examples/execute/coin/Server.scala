package examples.execute.coin

import lchannels.{In, LocalChannel, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.io.StdIn

object Server {
  def apply(Client: In[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) {
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

//S_game=rec X.(
// &{?Guess(num: Int)[num > 0 && num < 50].+{ !Correct(ans: Int)[ans==num].X, !Incorrect().X },
// ?New().X,
// ?Quit()} )
object MonitoredServer extends App {
  def run() = main(Array())
  val timeout = Duration.Inf

  val (in, out) = LocalChannel.factory[ExternalChoice1]()
  val mon = new Mon1(new CoinConnectionManager(), out, 300, 1.9599)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  Server(in)(global, timeout)
}
