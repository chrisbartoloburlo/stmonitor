package examples.smtp

import monitor.util.ConnectionManager

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{InetAddress, Socket}

class ServerConnectionManager(port: Int) extends ConnectionManager {
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val s = new Socket(InetAddress.getByName("localhost"), port)

  private val M220R = """220 ([\S]+) .*""".r
  private val M250R = """250 (.*)""".r
  private val M354R = """354 (.*)""".r
  private val M221R = """221 (.*)""".r

  def setup(): Unit = {
    println("[CM] Initialising sockets with SMTP Server")
    outB = new BufferedWriter(new OutputStreamWriter(s.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(s.getInputStream))
  }

  var m250counter = 1
  def receive(): Any = inB.readLine() match {
    case M220R(msg) => M220(msg)(null);
    case M250R(msg) =>
      if(m250counter == 1){
        m250counter += 1
        M250_13(msg)(null)
      } else if (m250counter == 2) {
        m250counter += 1
        M250_9(msg)(null)
      } else if (m250counter == 3) {
        m250counter += 1
        M250_1(msg)(null)
      } else if (m250counter == 4) {
        m250counter = 2
        M250_3(msg)(null)
      }
    case M354R(msg) => M354(msg)(null);
    case M221R(msg) => close(); M221_11(msg);
    case e => println(f"$e")
  }

  def send(x: Any): Unit = x match {
    case Helo(hostname) => outB.write(f"HELO ${hostname}\r\n"); outB.flush();
    case MailFrom(addr) => outB.write(f"MAIL FROM: ${addr}\r\n"); outB.flush();
    case RcptTo(addr) => outB.write(f"RCPT TO: ${addr}\r\n"); outB.flush();
    case Data() => outB.write(f"DATA\r\n"); outB.flush();
    case Content(txt) => outB.write(f"${txt}\r\n.\r\n"); outB.flush();
    case Quit_8() | Quit_12() | Quit_16() => outB.write(f"QUIT\r\n"); outB.flush();
    case e => print(f"$e"); close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    inB.close();
    outB.close();
  }
}
