package examples.auth

import lchannels.{SocketManager, SocketOut}
import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.Socket
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object MonWrapper extends App {
  val timeout = Duration.Inf

  class MonSocketManager(socket: Socket) extends SocketManager(socket) {
    private val inB = new BufferedReader(new InputStreamReader(in))
    private val SUCCR = """SUCC (.*)""".r
    private val RESR = """RES (.*)""".r
    private val TIMEOUTR = """TIMEOUT""".r
    private val FAILR = """FAIL (.*)""".r

    override def destreamer(): Any = inB.readLine() match {
//      case SUCCR(origTok) => Succ(origTok)(SocketOut[ExternalChoice1](this));
//      case RESR(content) => Res(content)(SocketOut[ExternalChoice1](this));
//      case TIMEOUTR() => Timeout()(SocketOut[Auth](this));
//      case FAILR(code) => Fail(code.toInt);
      case _ =>
    }

    private val outB = new BufferedWriter(new OutputStreamWriter(out))

    override def streamer(x: Any): Unit = x match {
//      case Auth(uname, pwd) => outB.write(f"AUTH $uname $pwd\r\n"); outB.flush();
//      case Get(resource, reqTok) => outB.write(f"GET $resource $reqTok\r\n"); outB.flush();
//      case Rvk(rvkTok) => outB.write(f"RVK $rvkTok\r\n"); outB.flush();
      case _ =>
    }
  }

  val serverPort = args(1).toInt //1335
  println("[Mon] Connecting to 127.0.0.1 " + serverPort)

  val serverConn = new Socket("127.0.0.1", serverPort)
  val monSktm = new MonSocketManager(serverConn)

  val clientPort = args(0).toInt //1330
  val clientConnectionManager = new ConnectionManager(clientPort)
//  val sChoice = SocketOut[Auth](monSktm)
//  val Mon = new Mon(clientConnectionManager, sChoice, 300)(global, timeout)
//  Mon.run()
}