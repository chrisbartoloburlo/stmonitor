package examples.pingpong

import lchannels.{SocketManager, SocketOut}

import java.net.Socket
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object MonWrapper extends App {
  val timeout = Duration.Inf

  class MonSocketManager(socket: Socket) extends SocketManager(socket) {

    override def destreamer(): Any = ???
//      TODO
//      case /pong => Pong();
//      case _ =>


    override def streamer(x: Any): Unit = x match {
//      TODO
//      case Ping() => ping response
//      case Quit() => quit response
      case _ =>
    }
  }

  val serverPort = args(1).toInt

  val serverConn = new Socket("127.0.0.1", serverPort)
  val monSktm = new MonSocketManager(serverConn)

  val clientPort = args(0).toInt
  val clientConnectionManager = new ClientConnectionManager(clientPort)
  val sChoice = SocketOut[ExternalChoice1](monSktm)
  val Mon = new Mon(clientConnectionManager, sChoice, 300)(global, timeout)
  Mon.run()
}