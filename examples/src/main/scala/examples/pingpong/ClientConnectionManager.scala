package examples.pingpong

import java.net.{Socket => JSocket}
import java.util.concurrent.{LinkedTransferQueue => Fifo}

import rawhttp.core.{RawHttp, RawHttpRequest}

import monitor.util.ConnectionManager

class ClientConnectionManager(cleanup: () => Unit) extends ConnectionManager {
  private val http = new RawHttp()
  private var queue = new Fifo[(RawHttpRequest, JSocket)]()
  private var socket: Option[JSocket] = None

  /** Add a new HTTP request and socket to be processed.
   * 
   * @param r HTTP request to process
   * @param s Socket connected to the HTTP client that sent `r`.
   */
  def queueRequest(r: RawHttpRequest, s: JSocket): Unit = {
    queue.add((r,s))
  }

  override def setup(): Unit = {
    // Nothing to do here
  }

  override def receive(): Any = {
    val (req, client) = queue.take()
    this.socket = Some(client) // For later use in send()
    
    val path = req.getUri().getPath()
    if (path.equals("/ping")) {
      Ping()(null)
    } else if (path.equals("/quit")) {
      http.parseResponse("HTTP/1.1 200 OK\n" +
        "Content-Type: text/plain\n" +
        "Content-Length: 0" +
        "\n").writeTo(client.getOutputStream())
      close()
      cleanup()
      Quit()
    } else {
      close()
      cleanup()
      return new RuntimeException(f"Invalid HTTP request to: ${req.getUri()}")
    }
  }

  override def send(x: Any): Unit = x match {
    case _: Pong => {
        http.parseResponse("HTTP/1.1 200 OK\n" +
          "Content-Type: text/plain\n" +
          "Content-Length: 4\n" +
          "\n" +
          "pong").writeTo(this.socket.get.getOutputStream())
          socket.get.close()
      }
    case _ => {
      close()
      throw new IllegalArgumentException("Unsupported message: ${x}")
    }
  }

  override def close(): Unit = {
    if (!socket.isEmpty) {
      socket.get.close()
    }
  }
}
