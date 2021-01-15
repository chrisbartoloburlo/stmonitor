package examples.execute.pingpong

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{InetAddress, ServerSocket, Socket}

class ConnectionManager(){
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val mon = new ServerSocket(1025)

  private val PINGR = """PING""".r
  private val QUITR = """QUIT""".r

  def setup(): Unit = {
    val client = mon.accept()
    println("[CM] Waiting for requests on 127.0.0.1 1025")
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case PINGR() => Ping()(null);
    case QUITR() => Quit();
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Pong() => outB.write(f"PONG\r\n"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    inB.close();
    outB.close();
  }
}