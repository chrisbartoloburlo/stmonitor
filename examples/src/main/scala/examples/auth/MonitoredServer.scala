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

  val server = new java.net.ServerSocket(1330)

  while(true) {
  val client = server.accept()
    val cm = new ClientConnectionManager(client)

    val (in, out) = LocalChannel.factory[Auth]()
    val Monitor = new Monitor(cm, out, 300, report)(global, timeout)

    val ServerThread = new Thread {
      override def run(): Unit = {
        Server(in)(global, timeout)
      }
    }

    ServerThread.start()
    Monitor.run()
  }
}
