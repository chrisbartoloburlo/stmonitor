package examples.coin

import monitor.util.ConnectionManager

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.ServerSocket

class ClientConnectionManager() extends ConnectionManager {
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val server = new ServerSocket(1330)

  private val HEADSR = """HEADS""".r
  private val TAILSR = """TAILS""".r

  def setup(): Unit = {
    println("[CM] Waiting for a connection on 127.0.0.1 1330")
    val client = server.accept()
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case HEADSR() => Heads()(null);
    case TAILSR() => Tails()(null);
    case e => e
  }

  def send(x: Any): Unit = x match {
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    inB.close();
    outB.close();
  }
}