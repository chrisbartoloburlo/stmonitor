package examples.execute.game

import lchannels.{In, LocalChannel}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object Server {
  def apply(Client: In[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) {
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
        case n @ New() =>
          println(f"[SRV] Received New")
          ans = r.nextInt(50)
          println(f"[SRV] Answer is: $ans")
          cont = n.cont
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

  val (in, out) = LocalChannel.factory[ExternalChoice1]()
  val mon = new Mon(new GameConnectionManager(), out, 300, 0.6745)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  Server(in)(global, timeout)
}
