package examples.http

import java.nio.file.{Path, Paths}
import java.time.ZonedDateTime
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.duration._

import lchannels.In

class Worker(in: In[Request], root: Path)(implicit timeout: Duration)
  extends Runnable with StrictLogging {
  private def logTrace(msg: String): Unit = logger.trace(msg)
  private def logDebug(msg: String): Unit = logger.debug(msg)
  private def logInfo(msg: String): Unit = logger.info(msg)
  private def logWarn(msg: String): Unit = logger.warn(msg)
  private def logError(msg: String): Unit = logger.error(msg)

  private val serverName = "lchannels HTTP server"
  private val pslash = Paths.get("/") // Used to relativize request paths

  override def run(): Unit = {
    logInfo("Started.")

    val r = MPRequest(in)

    val (rpath, cont) = {
      try getRequest(r)
      catch {
        case e: java.util.concurrent.TimeoutException =>
          logInfo(f"Timeout error: ${e.getMessage}")
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