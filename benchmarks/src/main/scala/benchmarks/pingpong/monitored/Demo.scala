package benchmarks.pingpong.monitored

import akka.actor.{ActorSystem, Props}
import lchannels.LocalChannel

// curl -w "@curl-format.txt" -o /Desktop -s http://localhost:8080/ping

object Demo extends App{
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  val timeout = Duration.Inf

  val system = ActorSystem("System")

  val (in, out) = LocalChannel.factory[ExternalChoice1]()
  val Mon = system.actorOf(Props(new Mon(out)(global, timeout)), name="Mon")
  val ponger = new ponger(in)(global, timeout)

  Mon ! MonStart

  val pongerThread = new Thread {
    override def run(): Unit = {
      ponger.run()
      system.terminate()
    }
  }

  pongerThread.start()
}
