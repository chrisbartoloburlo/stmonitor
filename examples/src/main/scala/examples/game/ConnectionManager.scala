package examples.game

import java.io
import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.Socket

import com.typesafe.scalalogging.Logger

class ConnectionManager(){
  val logger: Logger = Logger("CM")
  var outB: BufferedWriter = _
  var inB: BufferedReader = _
  private val correctR = """CORRECT (.+)""".r
  private val incorrectR = """INCORRECT""".r

  def setup(): Unit ={
    logger.info("Connecting to 127.0.0.1:1337")
    val conn = new Socket("127.0.0.1", 1337)
    outB = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream))
    inB = new BufferedReader(new InputStreamReader(conn.getInputStream))
  }

  def receive(): io.Serializable = inB.readLine() match {
    case correctR(ans) => Correct(ans.toInt)
    case incorrectR() => Incorrect()(null)
    case e => e
  }

  def send(x: Any): Unit = x match {
    case Guess(num) => outB.write(f"GUESS ${num}\n"); outB.flush()
    case Quit() => outB.write("QUIT\n"); outB.flush();
    case _ => close(); throw new Exception()
  }

  def close(): Unit = {
    outB.flush()
    inB.close()
    outB.close()
  }
}