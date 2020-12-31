package benchmarks.smtp.monitored

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{InetAddress, Socket}

class ConnectionManager(){
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val s = new Socket(InetAddress.getByName("localhost"), 1025)

  private val M220R = """220 ([\S]+) .*""".r
  private val M250R = """250 (.*)""".r
  private val M354R = """354 (.*)""".r
  private val M221R = """221 (.*)""".r

  def setup(): Unit = {
    println("[CM] Connecting with SMTP server")
    outB = new BufferedWriter(new OutputStreamWriter(s.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(s.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case M220R(msg) => M220(msg)(null);
    case M250R(msg) => M250(msg);
    case M354R(msg) => M354(msg)(null);
    case M221R(msg) => close(); M221(msg);
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Helo(hostname) => outB.write(f"HELO ${hostname}\r\n"); outB.flush();
    case MailFrom(addr) => outB.write(f"MAIL FROM: ${addr}\r\n"); outB.flush();
    case RcptTo(addr) => outB.write(f"RCPT TO: ${addr}\r\n"); outB.flush();
    case Data() => outB.write(f"DATA\r\n"); outB.flush();
    case Content(txt) => outB.write(f"${txt}\r\n.\r\n"); outB.flush();
    case Quit_1() | Quit_2() | Quit_3() => outB.write(f"QUIT\r\n"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    inB.close();
    outB.close();
  }
}