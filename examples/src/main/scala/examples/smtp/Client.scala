package examples.smtp

import lchannels.LocalChannel

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object MonitoredClient  {
  val timeout = Duration.Inf

  def run(): Unit = main(Array())

  def main(args: Array[String]): Unit = { 
    val (in, out) = LocalChannel.factory[M220]()
    def report(msg: String): Unit = {
      println(msg)
    }
    val mon = new Monitor(new ServerConnectionManager(args(3).toInt), out, 300, report)(global, timeout) //25

    val monThread = new Thread {
      override def run(): Unit = {
        mon.run()
      }
    }
    monThread.start()
    ClientLogic(in, args(0), args(1).toInt, args(2).toInt)(global, timeout)
  }
}

object Client {
  import lchannels.{SocketIn, SocketManager, SocketOut}

  import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
  import java.net.Socket

  val timeout = Duration.Inf

  def run(): Unit = main(Array())

  def main(args: Array[String]): Unit = {
    println(f"[*] Connecting to 127.0.0.1:${args(3).toInt}...")
    val conn = new Socket("127.0.0.1", args(3).toInt)
    val sktm = new SMTPSocketManager(conn)
    val c = SocketIn[M220](sktm)
    ClientLogic(c, args(0), args(1).toInt, args(2).toInt)(global, timeout)
  }

  class SMTPSocketManager(socket: Socket) extends SocketManager(socket) {
    private val outB = new BufferedWriter(new OutputStreamWriter(out))

    override def streamer(x: Any): Unit = x match {
      case Helo(hostname) => outB.write(f"HELO ${hostname}\r\n"); outB.flush();
      case MailFrom(addr) => outB.write(f"MAIL FROM: ${addr}\r\n"); outB.flush();
      case RcptTo(addr) => outB.write(f"RCPT TO: ${addr}\r\n"); outB.flush();
      case Data() => outB.write(f"DATA\r\n"); outB.flush();
      case Content(txt) => outB.write(f"${txt}\r\n.\r\n"); outB.flush();
      case Quit_8() | Quit_12() | Quit_16() => outB.write(f"QUIT\r\n"); outB.flush();
      case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
    }

    private val inB = new BufferedReader(new InputStreamReader(in))
    private val M220_1_R = """220 ([\S]+)""".r
    private val M220_2_R = """220 ([\S]+) .*""".r
    private val M250R = """250 (.*)""".r
    private val M354R = """354 (.*)""".r
    private val M221R = """221 (.*)""".r

    var m250counter = 1

    override def destreamer(): Any = inB.readLine() match {
      case M220_1_R(msg) => M220(msg)(SocketOut[ExternalChoice3](this));
      case M220_2_R(msg) => M220(msg)(SocketOut[ExternalChoice3](this));
      case M250R(msg) =>
        if(m250counter == 1){
          m250counter += 1
          M250_13(msg)(SocketOut[ExternalChoice2](this))
        } else if (m250counter == 2) {
          m250counter += 1
          M250_9(msg)(SocketOut[ExternalChoice1](this))
        } else if (m250counter == 3) {
          m250counter += 1
          M250_1(msg)(SocketOut[ExternalChoice1](this))
        } else if (m250counter == 4) {
          m250counter = 2
          M250_3(msg)(SocketOut[ExternalChoice2](this))
        }
      case M354R(msg) => M354(msg)(SocketOut[Content](this));
      case M221R(msg) => close(); M221_11(msg);
      case e => println(f"$e")
    }
  }
}

