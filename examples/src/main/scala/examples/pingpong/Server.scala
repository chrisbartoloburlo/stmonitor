package examples.pingpong

import lchannels.{In, SocketIn, SocketManager}

import scala.concurrent.duration.Duration
import java.net.{ServerSocket, Socket}

class Server(Pinger: In[ExternalChoice1])(implicit timeout: Duration) extends Runnable {
  override def run(): Unit = {
    println("[Ponger] Ponger started, to terminate press CTRL+c")
    var resp = Pinger
    var exit = false
    while(!exit) {
      resp ? {
        case ping @ Ping() =>
          resp = ping.cont !! Pong()
        case quit @ Quit() =>
          exit = true
      }
    }
  }
}


object ServerWrapper extends App {
  val timeout = Duration.Inf

  class ServerSocketManager(socket: Socket) extends SocketManager(socket) {
    override def destreamer(): Any = {
//      TODO
//      case /ping => Ping()(SocketOut[Pong](this));
//      case /quit => Quit()
    }

    override def streamer(x: Any): Unit = x match {
//      TODO
//      case Pong() => pong response
//      case Quit() =>
      case _ =>
    }
  }

//  TODO we probably need to change this for HTTP
  val clientSocket = new ServerSocket(1020)
  val pinger = clientSocket.accept()
  val sktmgr = new ServerSocketManager(pinger)
  val sPinger = SocketIn[ExternalChoice1](sktmgr)

  val server = new Server(sPinger)(timeout)
  server.run()
}