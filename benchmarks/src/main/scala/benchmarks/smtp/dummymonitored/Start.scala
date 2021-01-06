package benchmarks.smtp.dummymonitored

import benchmarks.util.dummymon

import java.lang.Thread.sleep

object Start {
  def main(args: Array[String]): Unit = {

    val mc = new mailclient(args(0), args(1).toInt, args(2).toInt)
    val dm = new dummymon(1025, args(3), args(4).toInt)

    val dummymonThread = new Thread {
      override def run(): Unit = {
        dm.run()
      }
    }

    dummymonThread.start()
    sleep(2000)
    val mailclientThread = new Thread {
      override def run(): Unit = {
        mc.run()
      }
    }
    mailclientThread.start()
  }
}
