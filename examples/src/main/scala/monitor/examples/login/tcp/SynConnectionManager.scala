package monitor.examples.login.tcp

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.ServerSocket
import monitor.examples.login.{Login, Retry, Success}

class SynConnectionManager(){
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val server = new ServerSocket(1330)

  private val loginR = """LOGIN (.+) (.+) (.+)""".r
  private val ackR = """ACK""".r

  def setup(): Unit = {
    val client = server.accept()
    println("[CM] Waiting for requests on 127.0.0.1:1337")
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case loginR(uname, pwd, token) => sendACK(); Login(uname, pwd, token)(null);
    case e => sendACK(); e
  }

  private def sendACK(): Unit = {
    outB.write("ACK"); outB.flush()
  }

  def send(x: Any): Unit = x match {
    case Success(id) => outB.write(f"SUCCESS ${id}"); outB.flush(); receiveACK();
    case Retry() => outB.write(f"RETRY"); outB.flush(); receiveACK();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  private def receiveACK(): Unit = inB.readLine() match {
    case ackR() => Unit
  }

  def close(): Unit = {
    outB.flush();
    inB.close();
    outB.close();
  }
}