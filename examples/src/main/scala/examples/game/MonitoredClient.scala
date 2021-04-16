package examples.game

import lchannels.{In, LocalChannel, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.io.StdIn

import scala.concurrent.ExecutionContext.Implicits.global

object Client {
  def apply(server: Out[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) {
    var s = server
    var quit = false
    while (!quit) {
      print("Enter guess (-1 to quit): ")
      val num = StdIn.readInt()
      if(num == -1){
        println("[C] Sending Quit()")
        s ! Quit()
        quit = true

      } else {
        println("[C] Sending Guess(" + num + ")")
        val repc: In[InternalChoice1] = s !! Guess(num)

        repc ? {
          case Correct(ans) =>
            println(f"[C] Received 'Correct(${ans})'")
            quit = true

          case m @ Incorrect() =>
            println(f"[C] Received 'Incorrect'")
            s = m.cont
        }
      }
    }
  }
}

object MonitoredClient extends App {
  def run() = main(Array())
  val timeout = Duration.Inf

  val (in, out) = LocalChannel.factory[ExternalChoice1]()
  def report(msg: String): Unit = {
    println(msg)
  }
  val mon = new Monitor(new ServerConnectionManager(), in, 300, report)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  Client(out)(global, timeout)
}
