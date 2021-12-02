package examples.game

import lchannels.{In, LocalChannel, SocketIn, SocketManager, SocketOut}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object ServerLogic {
  def apply(Client: In[InternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) {
    val r = scala.util.Random
    var ans = r.nextInt(50)
    println(f"[SRV] Answer is: $ans")
    var quit = false
    var cont = Client
    while (!quit) {
      cont ? {
        case g @ Guess(_) =>
          println(f"[SRV] Received Guess ${g.num}")
          if (g.num == ans) {
            println(f"[SRV] Sending Correct")
            cont = g.cont !! Correct()
          } else {
            println(f"[SRV] Sending Incorrect")
            cont = g.cont !! Incorrect()
          }
        case h @ Help() =>
          println(f"[SRV] Received Help")
          cont = h.cont !! Hint(f"Ans modulo 5 is: ${ans % 5}")
        case Quit() =>
          println("[SRV] Quitting")
          quit=true
      }
    }
  }
}

object MonitoredServer extends App {
  def run() = main(Array())
  val timeout = Duration.Inf

  val (in, out) = LocalChannel.factory[InternalChoice1]()
  val clientPort = args(0).toInt
  val zvalue = args(1).toFloat
  val mon = new Monitor(new ClientConnectionManager(clientPort), out, 300, zvalue, args(2).toBoolean)(global, timeout)

  val monThread = new Thread {
    override def run(): Unit = {
      mon.run()
    }
  }

  monThread.start()
  ServerLogic(in)(global, timeout)
}

object Server {
  def run(): Unit = main(Array())
  val timeout: Duration.Infinite = Duration.Inf
  val port = 1330
  val server = new ServerSocket(port)

  def main(args: Array[String]): Unit = {
    println(f"[CM] Waiting for a connection on 127.0.0.1 $port")
    val client = server.accept()
    val sktm = new GameSocketManager(client)
    val c = SocketIn[InternalChoice1](sktm)
    ServerLogic(c)(global, timeout)
  }

  class GameSocketManager(socket: Socket) extends SocketManager(socket) {
    private val outB = new BufferedWriter(new OutputStreamWriter(out))

    override def streamer(x: Any): Unit = x match {
      case Correct() => outB.write(f"CORRECT\r\n"); outB.flush();
      case Incorrect() => outB.write(f"INCORRECT\r\n"); outB.flush();
      case Hint(info) => outB.write(f"HINT $info\r\n"); outB.flush();
      case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
    }

    private val inB = new BufferedReader(new InputStreamReader(in))
    private val GUESSR = """GUESS (.*)""".r
    private val HELPR = """HELP""".r
    private val QUITR = """QUIT""".r

    override def destreamer(): Any = inB.readLine() match {
      case GUESSR(num) => Guess(num.toInt)(SocketOut[ExternalChoice1](this));
      case HELPR() => Help()(SocketOut[Hint](this));
      case QUITR() => Quit();
      case e => e
    }
  }
}