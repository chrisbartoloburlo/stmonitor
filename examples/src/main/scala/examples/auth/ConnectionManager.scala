package examples.auth

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.ServerSocket

class ConnectionManager(port: Int){
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val server = new ServerSocket(port)

  private val authR = """AUTH (.+) (.+)""".r

  def setup(): Unit = {
    val client = server.accept()
    println("[CM] Waiting for requests on 127.0.0.1:1337")
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
//    case authR(uname, pwd) => Auth(uname, pwd)(null);
    case e => e
  }

  def send(x: Any): Unit = x match {
//    case Succ(tok) => outB.write(f"SUCC ${tok}"); outB.flush();
//    case Fail(code) => outB.write(f"FAIL ${code}"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    outB.flush();
    inB.close();
    outB.close();
  }
}