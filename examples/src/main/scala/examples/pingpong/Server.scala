package examples.pingpong

import lchannels.{In, HttpServerIn, HttpServerOut, HttpServerManager}

import scala.concurrent.duration.Duration
import java.net.{ServerSocket, Socket}

class Server(pinger: In[ExternalChoice1])(implicit timeout: Duration) extends Runnable {
  override def run(): Unit = {
    println("[Ponger] Ponger started, to terminate press CTRL+c")
    var resp = pinger
    var exit = false
    while(!exit) {
      resp ? {
        case ping @ Ping() =>
          resp = ping.cont !! Pong()
        case quit @ Quit() =>
          println("Quitting")
          exit = true
      }
    }
  }
}


object ServerWrapper {
  import rawhttp.core.RawHttpRequest
  val timeout = Duration.Inf

  class PingPongManager() extends HttpServerManager() {
    override def request(r: RawHttpRequest): Any = {
      if (r.getUri().getPath().equals("/ping")) {
        Ping()(HttpServerOut[Pong](this))
      } else if (r.getUri().getPath().equals("/quit")) {
        sendResponse("HTTP/1.1 200 OK\n" +
          "Content-Type: text/plain\n" +
          "Content-Length: 0" +
          "\n")
        Quit()
      } else {
        throw new RuntimeException("Unsupported HTTP request to: ${request.getUri()}")
      }
    }

    override def response(x: Any): String = x match {
      case _: Pong => {
        "HTTP/1.1 200 OK\n" +
          "Content-Type: text/plain\n" +
          "Content-Length: 4\n" +
          "\n" +
          "pong"
      }
      case _ => {
        close()
        throw new IllegalArgumentException("Unsupported message: ${x}")
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val s = new ServerSocket(8080)
    while (true) {
      val client = s.accept()
      val t = new Thread { override def run = handler(client) }
      t.start()
    }
  }

  val sessions = scala.collection.mutable.Map[String, HttpServerManager]()

  def handler(client: Socket): Unit = {
    println("Handler started")
    val http = new rawhttp.core.RawHttp()
    val request = http.parseRequest(client.getInputStream())
    val sessionIds = request.getHeaders().get("X-Session-Id")
    if (sessionIds.size() == 0) {
      http.parseResponse("HTTP/1.1 500 Internal Server Error\n" +
                          "Content-Type: text/plain\n" +
                          "Content-Length: 18\n" +
                          "\n" +
                          "Invalid session id").writeTo(client.getOutputStream())
      client.close()
      return
    } else {
      val sid = sessionIds.get(0)
      var manager: Option[PingPongManager] = None
      sessions.synchronized {
        if (sessions.keySet.contains(sid)) {
          // A server for this session is already running
          println("Updating running manager for session ${sid}")
          sessions.get(sid).get.updatehttpRequestSocket(http, request, client)
        } else {
          // There is no server for this session, we create one
          val mgr = new PingPongManager()
          manager = Some(mgr)
          mgr.updatehttpRequestSocket(http, request, client)
          sessions(sid) = mgr
        }
      }
      if (!manager.isEmpty) {
        val sPinger = HttpServerIn[ExternalChoice1](manager.get)
        val server = new Server(sPinger)(timeout)
        server.run()
      }
    }
  }
}
