package examples.game

import monitor.util.ConnectionManager

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.ServerSocket

class ClientConnectionManager(port: Int) extends ConnectionManager {
  var outB: BufferedWriter = _
  var inB: BufferedReader = _

  val server = new ServerSocket(port)

  private val GUESSR = """GUESS (.*)""".r
  private val HELPR = """HELP""".r
  private val QUITR = """QUIT""".r

  def setup(): Unit = {
    println(f"[CM] Waiting for a connection on 127.0.0.1 $port")
    val client = server.accept()
    outB = new BufferedWriter(new OutputStreamWriter(client.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(client.getInputStream))
  }

  def receive(): Any = inB.readLine() match {
    case GUESSR(num) => Guess(num.toInt)(null);
    case HELPR() => Help()(null);
    case QUITR() => Quit();
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Correct() => outB.write(f"CORRECT\r\n"); outB.flush();
    case Incorrect() => outB.write(f"INCORRECT\r\n"); outB.flush();
    case Hint(info) => outB.write(f"HINT $info\r\n"); outB.flush();
    case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
  }

  def close(): Unit = {
    inB.close();
    outB.close();
  }
}