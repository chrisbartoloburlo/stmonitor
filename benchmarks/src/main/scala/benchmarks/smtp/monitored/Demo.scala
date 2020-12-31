package benchmarks.smtp.monitored

import akka.actor.{ActorSystem, Props}
import lchannels.LocalChannel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Demo {
  def main(args: Array[String]): Unit = {

    val timeout = Duration.Inf

    val system = ActorSystem("System")

    val (in, out) = LocalChannel.factory[M220]()
    val Mon = system.actorOf(Props(new Mon(out)(global, timeout)), name="Mon")
    val mc = new mailclient(in, args(0), args(1).toInt, args(2).toInt)(global, timeout)

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
