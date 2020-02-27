package monitor.examples.game

import akka.actor.{ActorSystem, Props}
import lchannels.LocalChannel
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Demo extends App{
  val timeout = 30.seconds

  val system = ActorSystem("System")

  val (in, out) = LocalChannel.factory[InternalChoice1]()
  val Mon = system.actorOf(Props(new Mon(in)(global, timeout)), name="Mon")
  val client = new Client(out)(global, timeout)

  Mon ! MonStart

  val clientThread = new Thread {
    override def run(): Unit = {
      client.run()
    }
  }

  clientThread.start()
}
