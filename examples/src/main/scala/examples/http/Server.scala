package examples.http

import lchannels._

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.global

object UnsafeServer {
  def run(): Unit = main(Array())

  def main(args: Array[String]) {
    import java.net.{InetAddress, ServerSocket}
    import java.util.concurrent.{Executors, ExecutorService}

    val root = java.nio.file.FileSystems.getDefault.getPath("").toAbsolutePath

    val address = InetAddress.getByName(null)
    val port = 8080
    val ssocket = new ServerSocket(port, 1, address)

    println(f"[*] HTTP server listening on: http://${address.getHostAddress}:${port}/")
    println(f"[*] Root directory: ${root}")
    println(f"[*] Press Ctrl+C to terminate")

    implicit val timeout: FiniteDuration = 30.seconds
    val nproc = math.max(Runtime.getRuntime().availableProcessors(), 4)
    val pool: ExecutorService = Executors.newFixedThreadPool(nproc)

    while (true) {
      val client = ssocket.accept()

      val mgr = new HttpServerSocketManager(client, true, println(_))
      val in = SocketIn[Request](mgr)
      pool.execute(new Worker(in, root))
    }
  }
}

object ServerWithMonitor {
  def run(): Unit = main(Array())

  def report(msg: String): Unit = {
    println(msg)
  }

  def main(args: Array[String]) {
    import java.net.{InetAddress, ServerSocket}
    import java.util.concurrent.{Executors, ExecutorService}

    val root = java.nio.file.FileSystems.getDefault.getPath("").toAbsolutePath

    val address = InetAddress.getByName(null)
    val port = 8080
    val ssocket = new ServerSocket(port, 1, address)

    println(f"[*] HTTP server with client monitor listening on: http://${address.getHostAddress}:${port}/")
    println(f"[*] Root directory: ${root}")
    println(f"[*] Press Ctrl+C to terminate")

    implicit val timeout: FiniteDuration = 30.seconds
    val nproc = math.max(Runtime.getRuntime().availableProcessors(), 4)
    val pool: ExecutorService = Executors.newFixedThreadPool(nproc)

    while (true) {
      val client = ssocket.accept()

      val cm = new ClientConnectionManager(client, true, println(_))
      val (in, out) = LocalChannel.factory[Request]()
      pool.execute(new Worker(in, root))
      pool.execute(new Monitor(cm, out, 300, report)(global, timeout))
    }
  }
}
