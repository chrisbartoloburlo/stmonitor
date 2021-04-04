package examples.pingpong

import lchannels.{LocalChannel}

import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

import scala.concurrent.duration.Duration
import scala.collection.mutable

object MonWrapper {
  val timeout = Duration.Inf

  val sessions = mutable.Map[String, ClientConnectionManager]()

  def main(args: Array[String]): Unit = {
    var nproc = Runtime.getRuntime().availableProcessors()
    if (nproc < 4) nproc = 4
    val pool: ExecutorService = Executors.newFixedThreadPool(nproc)

    val s = new ServerSocket(8080)
    println(s"[Ponger] Ponger started with ${nproc} handler threads; to terminate press CTRL+C")
    while (true) {
      val client = s.accept()
      pool.execute(new Handler(client, onNewSessionLocalServer(_)(pool)))
    }
  }

  // Intantiate a server thread together with the new manager and monitor
  def onNewSessionLocalServer(mgr: ClientConnectionManager)(pool: ExecutorService): Unit = {
    // A manager has been created, let's instantiate the monitor too
    val ec =  scala.concurrent.ExecutionContext.global
    val timeout = Duration.Inf
    val (in, out) = LocalChannel.factory[ExternalChoice1]
    val mon = new Mon(mgr, out, 300)(ec, timeout)
    val server = new Server(in)(timeout)
    pool.execute(server)
    mon.run()
  }

  class Handler(client: Socket,
                onNewSession: (ClientConnectionManager) => Unit) extends Runnable {
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
          onNewSession(manager.get)
        }
      }
    }
  }
}
