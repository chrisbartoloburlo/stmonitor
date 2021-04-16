package examples.auth

import lchannels.LocalChannel

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object MonitoredServer extends App {
  def run() = main(Array())
  val timeout = Duration.Inf

  def report(msg: String): Unit = {
    println(msg)
  }

  val CM = new ClientConnectionManager(1330)
  while(true) {
    val (in, out) = LocalChannel.factory[Auth]()
    val Monitor = new Monitor(CM, out, 300, report)(global, timeout)

    val ServerThread = new Thread {
      override def run(): Unit = {
        Server(in)(global, timeout)
      }
    }

    ServerThread.start()
    Monitor.run()
  }
}
