package examples.execute.pingpong

import lchannels.{LocalChannel, SocketManager, SocketOut}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{ServerSocket, Socket}


object Demo extends App{
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  val timeout = Duration.Inf

  val (lIn, lOut) = LocalChannel.factory[ExternalChoice1]()

  class MonSocketManager(socket: Socket) extends SocketManager(socket) {
    private val inB = new BufferedReader(new InputStreamReader(in))
    private val PONGR = """PONG""".r
    override def destreamer(): Any = inB.readLine() match {
      case PONGR() => Pong()(SocketOut[ExternalChoice1](this));
    }

    private val outB = new BufferedWriter(new OutputStreamWriter(out))
    override def streamer(x: Any): Unit = x match {
      case Ping() => outB.write(f"PING\r\n"); outB.flush();
      case Quit() => outB.write(f"QUIT\r\n"); outB.flush();
    }
  }

  val pongerThread = new Thread {
    override def run(): Unit = {
      SocketPonger.run()
    }
  }

  val MonThread = new Thread {
    override def run(): Unit = {
      println("[Mon] Accepting on 127.0.0.1:1020")
      val pongerSocket = new ServerSocket(1020)
      val ponger = pongerSocket.accept()
      val monSktm = new MonSocketManager(ponger)
      val sPong = SocketOut[ExternalChoice1](monSktm)
      val Mon = new Mon(sPong)(global, timeout)
      Mon.run()
    }
  }

  MonThread.start()
  pongerThread.start()
}
