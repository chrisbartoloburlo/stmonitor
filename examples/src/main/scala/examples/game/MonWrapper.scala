package examples.game

import lchannels.{SocketManager, SocketOut}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.Socket
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object MonWrapper extends App {
  val timeout = Duration.Inf

  class MonSocketManager(socket: Socket) extends SocketManager(socket) {
    private val inB = new BufferedReader(new InputStreamReader(in))
    private val CORRECTR = """CORRECT""".r
    private val INCORRECTR = """INCORRECT""".r
    private val HINTR = """HINT (.*)""".r
    override def destreamer(): Any = inB.readLine() match {
      case CORRECTR() => Correct()(SocketOut[InternalChoice1](this));
      case INCORRECTR() => Incorrect()(SocketOut[InternalChoice1](this));
      case HINTR(info) => Hint(info)(SocketOut[InternalChoice1](this))
      case _ =>
    }

    private val outB = new BufferedWriter(new OutputStreamWriter(out))
    override def streamer(x: Any): Unit = x match {
      case Guess(num) => outB.write(f"GUESS $num\r\n"); outB.flush();
      case Help() => outB.write(f"HELP\r\n"); outB.flush();
      case Quit() => outB.write(f"QUIT\r\n"); outB.flush();
      case _ =>
    }
  }

  val serverPort = args(1).toInt
  println("[Mon] Connecting to 127.0.0.1 "+serverPort)

  val serverConn = new Socket("127.0.0.1", serverPort)
  val monSktm = new MonSocketManager(serverConn)
  val sChoice = SocketOut[InternalChoice1](monSktm)

  val clientPort = args(0).toInt
  val zvalue = args(2).toFloat
  val log = args(3).toBoolean
  val clientConnectionManager = new ClientConnectionManager(clientPort)
  val Mon = new Monitor(clientConnectionManager, sChoice, 300, zvalue, log)(global, timeout)
  Mon.run()
}
