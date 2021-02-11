package examples.execute.game

import monitor.util.ConnectionManager

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.ServerSocket

class GameConnectionManager() extends ConnectionManager {
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val server = new ServerSocket(1330)

  private val GUESSR = """GUESS (.*)""".r
  private val NEWR = """NEW""".r
  private val QUITR = """QUIT""".r

  def setup(): Unit = {
    println("[CM] Waiting for a connection on 127.0.0.1 1330")
    val client = server.accept()
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case GUESSR(num) => Guess(num.toInt)(null);
    case NEWR() => New()(null);
    case QUITR() => Quit();
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Correct() => outB.write(f"CORRECT\r\n"); outB.flush();
    case Incorrect() => outB.write(f"INCORRECT\r\n"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    inB.close();
    outB.close();
  }
}