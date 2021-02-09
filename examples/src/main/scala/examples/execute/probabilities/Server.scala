package examples.execute.probabilities

import lchannels.{In, LocalChannel, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.io.StdIn

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
            cont = g.cont !! Correct(ans)
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

//S_game=rec X.(
// &{?Guess(num: Int)[0.7].+{ !Correct(ans: Int)[0.2].X, !Incorrect()[*].X },
// ?New()[0.2].X,
// ?Quit()[0.1]} )
object MonitoredServer extends App {
  def run() = main(Array())
  val timeout = Duration.Inf

  val (in, out) = LocalChannel.factory[ExternalChoice1]()
  val mon = new MonCI(new GameConnectionManager(), out, 300, 0.6745)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  Server(in)(global, timeout)
}
