package monitor.examples.game

import lchannels.{In, Out}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.io.StdIn

class Client (var Server: Out[InternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
  override def run(): Unit = {
    var quit = false
    while (!quit) {
      print("Enter guess (-1 to quit): ")
      val num = StdIn.readInt()
      if(num == -1){
        println("[C] Sending Quit()")
        Server ! Quit()
        quit = true

      } else {
        println("[C] Sending Guess(" + num + ")")
        val repc: In[ExternalChoice1] = Server !! Guess(num)

        repc ? {
          case Correct(ans) =>
            println(f"[C] Received 'Correct(${ans})'")
            quit = true

          case m @ Incorrect() =>
            println(f"[C] Received 'Incorrect'")
            Server = m.cont
        }
      }
    }
  }
}
