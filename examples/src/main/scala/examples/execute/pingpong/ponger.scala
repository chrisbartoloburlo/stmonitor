package examples.execute.pingpong

import lchannels.{In, SocketIn, SocketManager, SocketOut}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.Socket
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object ponger {
  def apply(Pinger: In[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration): Unit = {
    println("[PONGER] Ponger started, to terminate press CTRL+c")
    var resp = Pinger
    var exit = false
    while(!exit) {
      resp ? {
        case ping @ Ping() =>
          println("[PONGER] Received Ping()")
          resp = ping.cont !! Pong()
          println("[PONGER] Sending Pong()")
        case quit @ Quit() =>
          exit = true
      }
    }
  }
}

object SocketPonger extends App {
  def run(): Unit = main(Array())

  implicit val timeout = Duration.Inf

  class PongerSocketManager(socket: Socket) extends SocketManager(socket) {

    private val inB = new BufferedReader(new InputStreamReader(in))
    private val PINGR = """PING""".r
    private val QUITR = """QUIT""".r
    override def destreamer(): Any = inB.readLine() match {
      case PINGR() => Ping()(SocketOut[Pong](this));
      case QUITR() => Quit()
    }

    private val outB = new BufferedWriter(new OutputStreamWriter(out))
    override def streamer(x: Any): Unit = x match {
      case Pong() => outB.write(f"PONG\r\n"); outB.flush();
    }
  }

  println("[PONGER] Connecting to 127.0.0.1:1020")
  val conn = new Socket("127.0.0.1", 1020)
  val sktm = new PongerSocketManager(conn)
  val monIn = SocketIn[ExternalChoice1](sktm)
  ponger(monIn)(global, timeout)
}