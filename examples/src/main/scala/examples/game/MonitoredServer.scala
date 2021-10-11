package examples.game

import lchannels.{In, LocalChannel}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object Server {
  def apply(Client: In[InternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) {
    val r = scala.util.Random
    var ans = r.nextInt(50)
    println(f"[SRV] Answer is: $ans")
    var quit = false
    var cont = Client
    while (!quit) {
      cont ? {
        case g @ Guess(_) =>
          println(f"[SRV] Received Guess ${g.num}")
          if (g.num == ans) {
            println(f"[SRV] Sending Correct")
            cont = g.cont !! Correct()
          } else {
            println(f"[SRV] Sending Incorrect")
            cont = g.cont !! Incorrect()
          }
        case h @ Help() =>
          println(f"[SRV] Received Help")
          cont = h.cont !! Hint(f"Ans modulo 5 is: ${ans % 5}")
        case Quit() =>
          println("[SRV] Quitting")
          quit=true
      }
    }
  }
}

object MonitoredServer extends App {
  def run() = main(Array())
  val timeout = Duration.Inf

  val (in, out) = LocalChannel.factory[InternalChoice1]()
  val mon = new Monitor(new ClientConnectionManager(1330), out, 300, 0.6745)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  Server(in)(global, timeout)
}
