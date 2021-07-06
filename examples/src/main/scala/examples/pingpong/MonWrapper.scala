package examples.pingpong

import lchannels.{LocalChannel, HttpClientManager, HttpClientOut}

import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

import rawhttp.core.RawHttpResponse

import scala.concurrent.duration.Duration
import scala.collection.mutable

class PingPongManager(sessionId: String, host: String, port: Int) extends HttpClientManager(host, port) {
  override def response(r: RawHttpResponse[_]): Any = {
    if ((r.getStatusCode() == 200) &&
        (r.eagerly().getBody().map[String](_.toString)
         .orElseThrow(() => new RuntimeException("No response body")) == "pong")) {
      Pong()(HttpClientOut(this))
    } else {
      throw new RuntimeException(f"Invalid HTTP response: ${r.toString()}")
    }
  }

  override def request(x: Any): (String, Option[RawHttpResponse[_] => Unit]) = x match {
    case _: Ping => {
      ("GET /ping HTTP/1.1\r\n" +
       f"Host: ${host}\r\n" +
       f"X-Session-Id: ${sessionId}",
       None)
    }
    case _: Quit => {
      ("GET /quit HTTP/1.1\r\n" +
       f"Host: ${host}\r\n" +
       f"X-Session-Id: ${sessionId}",
       Some((resp: RawHttpResponse[_]) => {
         if (resp.getStatusCode() == 200) {
           // Nothing to do here
         } else {
           throw new RuntimeException(f"Invalid HTTP response to /quit: ${resp}}")
         }
       }))
    }
    case _ => {
      finalize()
      throw new IllegalArgumentException("Unsupported message: ${x}")
    }
  }
}

object MonWrapper {
  val timeout = Duration.Inf

  val sessions = mutable.Map[String, ClientConnectionManager]()

  def main(args: Array[String]): Unit = {
    val nproc = Math.max(Runtime.getRuntime().availableProcessors(), 4)
    val pool: ExecutorService = Executors.newFixedThreadPool(nproc)
    val listenPort = args(0).toInt

    def onNewSession(sessionId: String, mgr: ClientConnectionManager): Unit = {
      if (args.length == 1) {
        onNewSessionLocalServer(sessionId, mgr)(pool)
      } else {
        onNewSessionRemoteServer(sessionId, mgr)(args(1), args(2).toInt, pool)
      }
    }

    val s = new ServerSocket(listenPort)
    println(s"Monitor started with ${nproc} handler threads; to terminate press CTRL+C")
    while (true) {
      val client = s.accept()
      pool.execute(new Handler(client, onNewSession))
    }
  }

  // Intantiate a server thread together with the new manager and monitor
  def onNewSessionLocalServer(sessionId: String, mgr: ClientConnectionManager)
                             (pool: ExecutorService): Unit = {
    // A manager has been created, let's instantiate the monitor too
    val ec =  scala.concurrent.ExecutionContext.global
    val timeout = Duration.Inf
    val (in, out) = LocalChannel.factory[InternalChoice1]
    def report(msg: String): Unit = {
      println(msg)
    }
    val mon = new Monitor(mgr, out, 300, report)(ec, timeout)
    val server = new ServerLogic(in)(timeout)
    pool.execute(server)
    mon.run()
  }

  // At each new session, connect to a remote server
  def onNewSessionRemoteServer(sessionId: String, mgr: ClientConnectionManager)
                              (host: String, port: Int, pool: ExecutorService): Unit = {
    // A manager has been created, let's instantiate the monitor too
    val ec =  scala.concurrent.ExecutionContext.global
    val timeout = Duration.Inf
    val httpClientMgr = new PingPongManager(sessionId, host, port)
    val out = HttpClientOut[InternalChoice1](httpClientMgr)
    def report(msg: String): Unit = {
      println(msg)
    }
    val mon = new Monitor(mgr, out, 300, report)(ec, timeout)
    mon.run()
  }

  class Handler(client: Socket,
                onNewSession: (String, ClientConnectionManager) => Unit) extends Runnable {
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
        var manager: Option[ClientConnectionManager] = None
        sessions.synchronized {
          if (sessions.keySet.contains(sid)) {
            // A monitor (and manager) for this session is already running
            sessions(sid).queueRequest(request, client)
          } else {
            // There is no monitor nor manager for this session, we create one
            val mgr = new ClientConnectionManager(() => sessions.remove(sid))
            manager = Some(mgr)
            mgr.queueRequest(request, client)
            sessions(sid) = mgr
          }
        }
        if (manager.isDefined) {
          onNewSession(sid, manager.get)
        }
      }
    }
  }
}
