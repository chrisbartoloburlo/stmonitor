package benchmarks.smtp.dummymonitored

import akka.actor._
import lchannels.{In, Out}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{InetAddress, ServerSocket, Socket}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

case class MonStart()

class Mon(listenPort: Int, serverAddr: String, forwardPort: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
  def receive: Receive = {
    case MonStart =>
      val sListen = new ServerSocket(listenPort)
      val sender = sListen.accept()
      val outLB: BufferedWriter = new BufferedWriter(new OutputStreamWriter(sender.getOutputStream))
      val inLB: BufferedReader = new BufferedReader(new InputStreamReader(sender.getInputStream))

      val sForward = new Socket(InetAddress.getByName(serverAddr), forwardPort)
      val outFB: BufferedWriter = new BufferedWriter(new OutputStreamWriter(sForward.getOutputStream))
      val inFB: BufferedReader = new BufferedReader(new InputStreamReader(sForward.getInputStream))

      println("[Mon] Dummy Monitor started")
      forwardToListen(outLB, inLB, outFB, inFB)
  }

  val M220R = """220 (.*)""".r
  val M250R = """250 (.*)""".r
  val M354R = """354 (.*)""".r
  val M221R = """221 (.*)""".r

  val HELOR = """HELO (.*)""".r
  val MAILFROMR = """MAIL FROM: (.*)""".r
  val RCPTTOR = """RCPT TO: (.*)""".r
  val DATAR = """DATA""".r
  val PINGR = """ping (.*)""".r
  val QUITR = """QUIT""".r


  def listenToForward(outLB: BufferedWriter, inLB: BufferedReader, outFB: BufferedWriter, inFB: BufferedReader): Any = {
    inLB.readLine() match {
      case HELOR(hostname) =>
        outFB.write(f"HELO ${hostname}\r\n"); outFB.flush();
        forwardToListen(outLB, inLB, outFB, inFB)

      case MAILFROMR(mail) =>
        outFB.write(f"MAIL FROM: ${mail}\r\n"); outFB.flush();
        forwardToListen(outLB, inLB, outFB, inFB)

      case RCPTTOR(mail) =>
        outFB.write(f"RCPT TO: ${mail}\r\n"); outFB.flush();
        forwardToListen(outLB, inLB, outFB, inFB)

      case DATAR() =>
        outFB.write(f"DATA\r\n"); outFB.flush();
        forwardToListen(outLB, inLB, outFB, inFB)

      case PINGR(i) =>
        outFB.write(f"ping ${i}\r\n.\r\n"); outFB.flush();
        forwardToListen(outLB, inLB, outFB, inFB)

      case QUITR() =>
        outFB.write(f"QUIT\r\n"); outFB.flush();
        forwardToListen(outLB, inLB, outFB, inFB)

      case e =>
//        println(e)
    }
  }

  def forwardToListen(outLB: BufferedWriter, inLB: BufferedReader, outFB: BufferedWriter, inFB: BufferedReader): Any = {
    inFB.readLine() match {
      case M220R(msg) =>
        outLB.write(f"220 ${msg}\r\n"); outLB.flush()
        listenToForward(outLB, inLB, outFB, inFB)

      case M250R(msg) =>
        outLB.write(f"250 ${msg}\r\n"); outLB.flush()
        listenToForward(outLB, inLB, outFB, inFB)

      case M354R(msg) =>
        outLB.write(f"354 ${msg}\r\n"); outLB.flush()
        listenToForward(outLB, inLB, outFB, inFB)

      case M221R(msg) =>
        outLB.write(f"221 ${msg}\r\n"); outLB.flush()
        listenToForward(outLB, inLB, outFB, inFB)

      case e =>
//        println(e)
    }

  }
}