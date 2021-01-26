package newbenchmarks.smtp.monitored

import lchannels.LocalChannel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Demo {
  def main(args: Array[String]): Unit = {

    val timeout = Duration.Inf

    val (in, out) = LocalChannel.factory[M220]()
    val mc = new mailclient(in, args(0), args(1).toInt, args(2).toInt)(global, timeout)
    val mon = new Mon(new SMTPConnectionManager(), out, 300)(global, timeout)

    val mailclientThread = new Thread {
      override def run(): Unit = {
        mc.run()
      }
    }

    val monThread = new Thread {
      override def run(): Unit = {
        mon.run()
      }
    }

    monThread.start()
    mailclientThread.start()
  }
}
