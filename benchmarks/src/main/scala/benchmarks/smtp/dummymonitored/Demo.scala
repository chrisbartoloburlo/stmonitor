package benchmarks.smtp.dummymonitored

import akka.actor.{ActorSystem, Props}
import lchannels.LocalChannel

import java.lang.Thread.sleep
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Demo {
  def main(args: Array[String]): Unit = {

    val timeout = Duration.Inf

    val system = ActorSystem("System")

    val mc = new mailclient(args(0), args(1).toInt, args(2).toInt)
    val Mon = system.actorOf(Props(new Mon(1025, args(3), args(4).toInt)(global, timeout)), name="Mon")

    Mon ! MonStart
    val mailclientThread = new Thread {
      override def run(): Unit = {
        mc.run()
        system.terminate()
      }
    }
    mailclientThread.start()
  }
}
