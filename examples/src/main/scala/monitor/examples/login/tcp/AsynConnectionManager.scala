package monitor.examples.login.tcp

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.ServerSocket
import monitor.examples.login.{Login, Retry, Success}

class AsynConnectionManager(){
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val server = new ServerSocket(1330)

  private val loginR = """LOGIN (.+) (.+) (.+)""".r

  def setup(): Unit = {
    val client = server.accept()
    println("[CM] Waiting for requests on 127.0.0.1:1337")
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case loginR(uname, pwd, token) => Login(uname, pwd, token)(null);
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Success(id) => outB.write(f"SUCCESS ${id}"); outB.flush();
    case Retry() => outB.write(f"RETRY"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    outB.flush();
    inB.close();
    outB.close();
  }
}