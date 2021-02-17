package examples.demo

import lchannels.{LocalChannel, SocketManager, SocketOut}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{ServerSocket, Socket}
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
      case CORRECTR() => Correct()(SocketOut[ExternalChoice1](this));
      case INCORRECTR() => Incorrect()(SocketOut[ExternalChoice1](this));
      case HINTR(info) => Hint(info)(SocketOut[ExternalChoice1](this))
    }

    private val outB = new BufferedWriter(new OutputStreamWriter(out))
    override def streamer(x: Any): Unit = x match {
      case Guess(num) => outB.write(f"GUESS $num\r\n"); outB.flush();
      case Help() => outB.write(f"HELP\r\n"); outB.flush();
      case Quit() => outB.write(f"QUIT\r\n"); outB.flush();
    }
  }
  println("[Mon] Connecting to 127.0.0.1 1337")
  //  val serverSocket = new ServerSocket(1020)
  //  val server = serverSocket.accept()
  //args[0]
  val conn = new Socket("127.0.0.1", 1337)
  val monSktm = new MonSocketManager(conn)
  val sChoice = SocketOut[ExternalChoice1](monSktm)

  //args[1] check socat
  val connectionManager = new GameConnectionManager(1330)
  val Mon = new Mon(connectionManager, sChoice, 300, 4.4172)(global, timeout)
  Mon.run()
}
