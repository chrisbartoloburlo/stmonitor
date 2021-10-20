package examples.smtp

import lchannels.{SocketManager, SocketOut}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object MonWrapper extends App {
  val timeout = Duration.Inf

  class MonSMTPSocketManager(socket: Socket) extends SocketManager(socket) {
    private val outB = new BufferedWriter(new OutputStreamWriter(out))

    override def streamer(x: Any): Unit = x match {
      case M220(msg) => outB.write(f"220 $msg\r\n"); outB.flush();
      case M250_13(msg) => outB.write(f"250 $msg\r\n"); outB.flush();
      case M250_9(msg) => outB.write(f"250 $msg\r\n"); outB.flush();
      case M250_1(msg) => outB.write(f"250 $msg\r\n"); outB.flush();
      case M250_3(msg) => outB.write(f"250 $msg\r\n"); outB.flush();
      case M354(msg) => outB.write(f"354 $msg\r\n"); outB.flush();
      case M221_7(msg) => outB.write(f"221 $msg\r\n"); outB.flush();
      case M221_11(msg) => outB.write(f"221 $msg\r\n"); outB.flush();
      case M221_15(msg) => outB.write(f"221 $msg\r\n"); outB.flush();
      case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
    }

    private val inB = new BufferedReader(new InputStreamReader(in))
    val HELOR = """HELO (.*)""".r
    val MAILFROMR = """MAIL FROM: (.*)""".r
    val RCPTTOR = """RCPT TO: (.*)""".r
    val DATAR = """DATA""".r
    val CONTENTR = """(.*)""".r
    val QUITR = """QUIT""".r

    var m250counter = 1
    override def destreamer(): Any = inB.readLine() match {
      case HELOR(hostname) => Helo(hostname)(SocketOut[M250_13](this));
      case MAILFROMR(addr) => MailFrom(addr)(SocketOut[M250_9](this));
      case RCPTTOR(addr) =>  RcptTo(addr)(SocketOut[M250_1](this));
      case DATAR() => Data()(SocketOut[M354](this));
      case QUITR() =>  Quit_12()(SocketOut[M221_11](this));
      case CONTENTR(txt) => inB.readLine(); Content(txt)(SocketOut[M250_3](this));
      case e => println(f"$e")
    }
  }

  val serverPort = args(0).toInt //25
  val serverConnectionManager = new ServerConnectionManager(serverPort)

  val clientPort = args(1).toInt //1025
  val server = new ServerSocket(clientPort)
  println(f"[Mon] Waiting for requests on 127.0.0.1 $clientPort")
  val client = server.accept()
  val clientSktm = new MonSMTPSocketManager(client)

  val log = args(2).toBoolean

  val sChoice = SocketOut[M220](clientSktm)
  def report(msg: String): Unit = {
    println(msg)
  }
  val Mon = new Monitor(serverConnectionManager, sChoice, 300, 0.6745, log)(global, timeout)
  Mon.run()
}