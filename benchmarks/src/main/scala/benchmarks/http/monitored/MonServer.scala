package benchmarks.http.monitored

import com.typesafe.scalalogging.StrictLogging
import lchannels._

import java.net.Socket
import java.nio.file.{Path, Paths}
import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

// Input message types for multiparty sessions
case class MPIRequest(p: RequestLine, cont: MPRequestChoice)

sealed abstract class MsgMPRequestChoice
case class MPAcceptEncodings(p: String, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPAccept(p: String, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPDoNotTrack(p: Boolean, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPUpgradeIR(p: Boolean, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPConnection(p: String, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPUserAgent(p: String, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPAcceptLanguage(p: String, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPHost(p: String, cont: MPRequestChoice) extends MsgMPRequestChoice
case class MPRequestBody(p: Body, cont: MPHttpVersion) extends MsgMPRequestChoice

// Output message types for multiparty sessions
case class MPOHttpVersion(p: Version)
case class MPCode404(p: String)
case class MPCode200(p: String)
case class MPETag(p: String)
case class MPServer(p: String)
case class MPContentLength(p: Int)
case class MPContentType(p: String)
case class MPVary(p: String)
case class MPVia(p: String)
case class MPStrictTS(p: String)
case class MPResponseBody(p: Body)
case class MPAcceptRanges(p: String)
case class MPLastModified(p: ZonedDateTime)
case class MPDate(p: ZonedDateTime)

case class MPRequest(c: In[Request]) {
  def receive(implicit timeout: Duration = Duration.Inf): MPIRequest = {
    c.receive(timeout) match {
      case m @ Request(p) =>
        MPIRequest(p, MPRequestChoice(m.cont))
    }
  }
}

case class MPRequestChoice(c: In[ExternalChoice1]) {
  def receive(implicit timeout: Duration = Duration.Inf): MsgMPRequestChoice = {
    c.receive(timeout) match {
      case m @ Accept(p) =>
        MPAccept(p, MPRequestChoice(m.cont))
      case m @ AcceptEncodings(p) =>
        MPAcceptEncodings(p, MPRequestChoice(m.cont))
      case m @ AcceptLanguage(p) =>
        MPAcceptLanguage(p, MPRequestChoice(m.cont))
      case m @ Connection(p) =>
        MPConnection(p, MPRequestChoice(m.cont))
      case m @ DoNotTrack(p) =>
        MPDoNotTrack(p, MPRequestChoice(m.cont))
      case m @ Host(p) =>
        MPHost(p, MPRequestChoice(m.cont))
      case m @ RequestBody(p) =>
        MPRequestBody(p, MPHttpVersion(m.cont))
      case m @ UpgradeIR(p) =>
        MPUpgradeIR(p, MPRequestChoice(m.cont))
      case m @ UserAgent(p) =>
        MPUserAgent(p, MPRequestChoice(m.cont))
    }
  }
}

case class MPHttpVersion(c: Out[HttpVersion]) {
  def send(v: MPOHttpVersion): MPCode200OrCode404 = {
    val cnt = c !! HttpVersion(v.p)_
    MPCode200OrCode404(cnt)
  }
}

case class MPCode200OrCode404(c: Out[InternalChoice3]) {
  def send(v: MPCode200): MPResponseChoice200 = {
    val cnt = c !! Code200(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPCode404): MPResponseChoice404 = {
    val cnt = c !! Code404(v.p)_
    MPResponseChoice404(cnt)
  }
}

case class MPResponseChoice200(c: Out[InternalChoice2]) {
  def send(v: MPAcceptRanges): MPResponseChoice200 = {
    val cnt = c !! AcceptRanges_29(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPContentLength): MPResponseChoice200 = {
    val cnt = c !! ContentLength_23(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPContentType): MPResponseChoice200 = {
    val cnt = c !! ContentType_24(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPDate): MPResponseChoice200 = {
    val cnt = c !! Date_31(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPETag): MPResponseChoice200 = {
    val cnt = c !! ETag_21(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPLastModified): MPResponseChoice200 = {
    val cnt = c !! LastModified_30(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPResponseBody): Unit = {
    c ! ResponseBody_28(v.p)
  }
  def send(v: MPServer): MPResponseChoice200 = {
    val cnt = c !! Server_22(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPStrictTS): MPResponseChoice200 = {
    val cnt = c !! StrictTS_27(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPVary): MPResponseChoice200 = {
    val cnt = c !! Vary_25(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPVia): MPResponseChoice200 = {
    val cnt = c !! Via_26(v.p)_
    MPResponseChoice200(cnt)
  }
}

case class MPResponseChoice404(c: Out[InternalChoice1]) {
  def send(v: MPAcceptRanges): MPResponseChoice404 = {
    val cnt = c !! AcceptRanges_17(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPContentLength): MPResponseChoice404 = {
    val cnt = c !! ContentLength_11(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPContentType): MPResponseChoice404 = {
    val cnt = c !! ContentType_12(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPDate): MPResponseChoice404 = {
    val cnt = c !! Date_19(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPETag): MPResponseChoice404 = {
    val cnt = c !! ETag_9(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPLastModified): MPResponseChoice404 = {
    val cnt = c !! LastModified_18(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPResponseBody): Unit = {
    c ! ResponseBody_16(v.p)
  }
  def send(v: MPServer): MPResponseChoice404 = {
    val cnt = c !! Server_10(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPStrictTS): MPResponseChoice404 = {
    val cnt = c !! StrictTS_15(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPVary): MPResponseChoice404 = {
    val cnt = c !! Vary_13(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPVia): MPResponseChoice404 = {
    val cnt = c !! Via_14(v.p)_
    MPResponseChoice404(cnt)
  }
}


object Server extends App {
  def run(): Unit = main(Array())

  import java.net.{InetAddress, ServerSocket}
  val root = java.nio.file.FileSystems.getDefault.getPath("").toAbsolutePath

  val address = InetAddress.getByName(null)
  val port = 8080
  val ssocket = new ServerSocket(port, 0, address)

  println(f"[*] HTTP server listening on: http://${address.getHostAddress}:${port}/")
  println(f"[*] Root directory: ${root}")
  println(f"[*] Press Ctrl+C to terminate")

  implicit val timeout: FiniteDuration = 30.seconds
  accept(1)

  @scala.annotation.tailrec
  def accept(nextWorkerId: Int) {
    val client = ssocket.accept()
    println(f"[*] Connection from ${client.getInetAddress}, spawning worker")
    new Worker(nextWorkerId, client, root)
    accept(nextWorkerId + 1)
  }
}

class Worker(id: Int, socket: Socket, root: Path)(implicit timeout: Duration)
  extends Runnable with StrictLogging {
  private def logTrace(msg: String): Unit = logger.trace(msg)
  private def logDebug(msg: String): Unit = logger.debug(msg)
  private def logInfo(msg: String): Unit = logger.info(msg)
  private def logWarn(msg: String): Unit = logger.warn(msg)
  private def logError(msg: String): Unit = logger.error(msg)

  private val serverName = "lchannels HTTP server"
  private val pslash = Paths.get("/") // Used to relativize request paths

  // Own thread
  private val thread = { val t = new Thread(this); t.start(); t }
  def join(): Unit = thread.join()

  override def run(): Unit = {
    logInfo("Started.")

    val cm = new ConnectionManager(socket, true, logInfo)
    val (in, out) = LocalChannel.factory[Request]()
    val mon = new Mon(cm, out, 300)(global, timeout)
    val monThread = new Thread {
      override def run(): Unit = {
        mon.run()
      }
    }

    val r = MPRequest(in)

    monThread.start()

    val (rpath, cont) = {
      try getRequest(r)
      catch {
        case cm.ConnectionClosed(msg) =>
          logInfo(msg)
          return
        case e: java.util.concurrent.TimeoutException =>
          logInfo(f"Timeout error: ${e.getMessage}")
          cm.close()
          logInfo("Terminating.")
          return
      }
    }

    val path = root.resolve(pslash.relativize(Paths.get(rpath)))
    logInfo(f"Resolved request path: ${path}")
    // TODO: we should reject paths like e.g. ../../../../etc/passwd

    val cont2 = cont.send(MPOHttpVersion(Http11))

    val file = path.toFile

    if (!file.exists || !file.canRead) {
      notFound(cont2, rpath)
    } else {
      logInfo("Resource found.")
      val cont3 = cont2.send(MPCode200("OK"))
        .send(MPServer(serverName))
        .send(MPDate(ZonedDateTime.now))
      if (file.isFile) {
        serveFile(cont3, path)
      } else if (file.isDirectory) {
        serveDirectory(cont3, rpath, file)
      } else {
        throw new RuntimeException(f"BUG: unsupported resource type: ${path}")
      }
    }

    logInfo("Terminating.")
  }

  private def getRequest(c: MPRequest)(implicit timeout: Duration) = {
    val req = c.receive
    logInfo(f"Method: ${req.p.method}; path: ${req.p.path}; version: ${req.p.version}")
    val cont = choices(req.cont)
    (req.p.path, cont)
  }

  @scala.annotation.tailrec
  private def choices(c: MPRequestChoice)(implicit timeout: Duration): MPHttpVersion = c.receive match {
    case MPAccept(p, cont)  =>
      logInfo(f"Client accepts: ${p}")
      choices(cont)
    case MPAcceptEncodings(p, cont)  =>
      logInfo(f"Client encodings: ${p}")
      choices(cont)
    case MPAcceptLanguage(p, cont)  =>
      logInfo(f"Client languages: ${p}")
      choices(cont)
    case MPConnection(p, cont)  =>
      logInfo(f"Client connection: ${p}")
      choices(cont)
    case MPDoNotTrack(p, cont)  =>
      logInfo(f"Client Do Not Track flag: ${p}")
      choices(cont)
    case MPHost(p, cont)  =>
      logInfo(f"Client host: ${p}")
      choices(cont)
    case MPRequestBody(p, cont)  =>
      logInfo(f"Client request body: ${p}")
      cont
    case MPUpgradeIR(p, cont)  =>
      logInfo(f"Client upgrade insecure requests: ${p}")
      choices(cont)
    case MPUserAgent(p, cont)  =>
      logInfo(f"Client user agent: ${p}")
      choices(cont)
  }

  private def notFound(c: MPCode200OrCode404, res: String) = {
    logInfo(f"Resource not found: ${res}")
    c.send(MPCode404("Not Found"))
      .send(MPServer(serverName))
      .send(MPDate(ZonedDateTime.now))
      .send(MPResponseBody(
        Body("text/plain", f"Resource ${res} not found".getBytes("UTF-8"))))
  }

  private def serveFile(c: MPResponseChoice200, file: Path) = {
    val filename = file.getFileName.toString
    val contentType = {
      if (filename.endsWith(".html")) "text/html"
      else if (filename.endsWith(".css")) "text/css"
      else "text/plain" // TODO: we assume content is human-readable
    }
    logInfo(f"Serving file: ${file} (content type: ${contentType})")

    // TODO: for simplicity, we assume all files are UTF-8
    c.send(MPResponseBody(
      Body(f"${contentType}; charset=utf-8", java.nio.file.Files.readAllBytes(file))))
  }

  private def serveDirectory(c: MPResponseChoice200, rpath: String, dir: java.io.File) = {
    logInfo(f"Serving directory: ${dir}")

    val list = dir.listFiles.foldLeft(""){(a,i) =>
      a + f"""|      <li>
              |        <a href="${i.getName}${if (i.isFile) "" else "/"}">
              |          ${i.getName}${if (i.isFile) "" else "/"}
              |        </a>
              |      </li>\n""".stripMargin
    }
    val html = f"""|<!DOCTYPE html>
                   |<html>
                   |  <head>
                   |    <meta charset="UTF-8">
                   |    <title>Contents of ${rpath}</title>
                   |  </head>
                   |  <body>
                   |    <h1>Contents of ${rpath}</h1>
                   |    <ul>
                   |${list}
                   |    </ul>
                   |    <p><em>Page generated by ${serverName}</em></p>
                   |  </body>
                   |</html>\n""".stripMargin

    c.send(MPResponseBody(
      Body("text/html", html.getBytes("UTF-8"))))
  }
}