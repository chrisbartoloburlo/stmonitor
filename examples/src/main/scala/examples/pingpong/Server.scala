package examples.pingpong

import lchannels.{HttpServerIn, HttpServerManager, HttpServerOut}

import scala.concurrent.duration.Duration
import java.net.{ServerSocket, Socket}
import scala.collection.mutable

object ServerWrapper {
  import rawhttp.core.RawHttpRequest
  val timeout = Duration.Inf

  val sessions = mutable.Map[String, HttpServerManager]()

  class PingPongManager(sessionId: String) extends HttpServerManager() {
    override def request(r: RawHttpRequest): Any = {
      val path = r.getUri().getPath()
      if (path.equals("/ping")) {
        Ping()(HttpServerOut[Pong](this))
      } else if (path.equals("/quit")) {
        sendResponse("HTTP/1.1 200 OK\n" +
          "Content-Type: text/plain\n" +
          "Content-Length: 0" +
          "\n")
        finalize()
        Quit()
      } else {
        throw new RuntimeException(f"Invalid HTTP request to: ${r.getUri()}")
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
        finalize()
        throw new IllegalArgumentException("Unsupported message: ${x}")
      }
    }

    final override def finalize() = sessions.synchronized {
      sessions.remove(sessionId)
      super.finalize()
    }
  }

  def main(args: Array[String]): Unit = {
    import java.util.concurrent.{Executors, ExecutorService}
    val nproc = Math.max(Runtime.getRuntime().availableProcessors(), 4)
    val pool: ExecutorService = Executors.newFixedThreadPool(nproc)
    val listenPort = if (args.length == 0) 8080 else args(0).toInt

    val s = new ServerSocket(listenPort)
    println(s"[Ponger] Ponger started ${nproc} max threads; to terminate press CTRL+C")
    while (true) {
      val client = s.accept()
      pool.execute(new Handler(client))
    }
  }

  class Handler(client: Socket) extends Runnable {
    override def run(): Unit = {
      val http = new rawhttp.core.RawHttp()
      val request = http.parseRequest(client.getInputStream)
      val sessionIds = request.getHeaders.get("X-Session-Id")
      if (sessionIds.size() == 0) {
        http.parseResponse("HTTP/1.1 500 Internal Server Error\n" +
                            "Content-Type: text/plain\n" +
                            "Content-Length: 18\n" +
                            "\n" +
                            "Invalid session id").writeTo(client.getOutputStream)
        client.close()
        return
      } else {
        val sid = sessionIds.get(0)
        var manager: Option[PingPongManager] = None
        sessions.synchronized {
          if (sessions.keySet.contains(sid)) {
            // A server for this session is already running
            sessions(sid).queueRequest(request, client)
          } else {
            // There is no server for this session, we create one
            val mgr = new PingPongManager(sid)
            manager = Some(mgr)
            mgr.queueRequest(request, client)
            sessions(sid) = mgr
          }
        }
        if (manager.isDefined) {
          val sPinger = HttpServerIn[InternalChoice1](manager.get)
          val server = new ServerLogic(sPinger)(timeout)
          server.run()
        }
      }
    }
  }
}
