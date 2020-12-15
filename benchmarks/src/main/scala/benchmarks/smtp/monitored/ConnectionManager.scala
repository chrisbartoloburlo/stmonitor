package benchmarks.smtp.monitored

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.ServerSocket

class ConnectionManager(){
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val server = new ServerSocket(1330)

//   (.+) (.+) (.+)
  private val M220R = """220 .*""".r
  private val M250R = """250 OK""".r
  private val M354R = """354 .*""".r
  private val M221R = """221 .*""".r

  def setup(): Unit = {
    val client = server.accept()
    println("[CM] Waiting for requests on 127.0.0.1:1337")
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
//    case loginR(uname, pwd, token) => Login(uname, pwd, token)(null);
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Helo(hostname) => outB.write(f"HELO ${hostname}"); outB.flush();
    case MailFrom(addr) => outB.write(f"MAIL FROM: ${addr}"); outB.flush();
    case RcptTo(addr) => outB.write(f"RCPT TO: ${addr}"); outB.flush();
    case Data() => outB.write(f"DATA"); outB.flush();
    case Content(txt) => outB.write(f"${txt}\r\n.\r\n"); outB.flush();
    case Quit_1() || Quit_2() || Quit_3() || Quit_4() => outB.write(f"QUIT"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    outB.flush();
    inB.close();
    outB.close();
  }
}