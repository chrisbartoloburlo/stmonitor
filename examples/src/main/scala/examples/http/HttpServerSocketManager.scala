package examples.http

import lchannels._
import java.net.Socket
import java.io.{
  BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter
}
import java.time.format.DateTimeFormatter.{RFC_1123_DATE_TIME => RFCDate}

/** lchannels ocket manager for the HTTP protocol.
 *  
 *  @param socket the socket managed by the instance
 *  @param relaxHeaders if true, skip unmanaged HTTP headers (otherwise, error)
 *  @param logger logging function, used to report e.g. skipped headers and other info 
 */
class HttpServerSocketManager(socket: Socket,
                              relaxHeaders: Boolean,
                              logger: (String) => Unit) extends SocketManager(socket) {
  case class ConnectionClosed(msg: String) extends java.io.IOException(msg)
  case class ProtocolError(msg: String) extends java.io.IOException(msg)
  
  private val outb = new BufferedWriter(new OutputStreamWriter(out))
  private var requestStarted = false // Remembers whether GET/POST/... was seen
  
  private val crlf = "\r\n"
  
  override def streamer(x: Any) = x match {
    case HttpVersion(v) => outb.write(f"${v} ")
    case Code200(msg) => outb.write(f"200 ${msg}${crlf}"); outb.flush()
    case Code404(msg) => outb.write(f"404 ${msg}${crlf}"); outb.flush()
    
    case Date(date) => outb.write(f"Date: ${date.format(RFCDate)}${crlf}"); outb.flush()
    case Date2(date) => outb.write(f"Date: ${date.format(RFCDate)}${crlf}"); outb.flush()
    case Server(server) => outb.write(f"Server: ${server}${crlf}"); outb.flush()
    case Server2(server) => outb.write(f"Server: ${server}${crlf}"); outb.flush()
    case ResponseBody(body) => {
      outb.write(f"Content-Type: ${body.contentType}${crlf}"); outb.flush()
      outb.write(f"Content-Length: ${body.contents.size}${crlf}${crlf}"); outb.flush()
      out.write(body.contents) // NOTE: bypass outb, to preserve encoding
      outb.close()
    }
    case ResponseBody2(body) => {
      outb.write(f"Content-Type: ${body.contentType}${crlf}"); outb.flush()
      outb.write(f"Content-Length: ${body.contents.size}${crlf}${crlf}"); outb.flush()
      out.write(body.contents) // NOTE: bypass outb, to preserve encoding
      outb.close()
    }
    
    case e => { close(); throw new RuntimeException(f"BUG: unsupported message: '${e}'") }
  }
  
  private val inb = new BufferedReader(new InputStreamReader(in))
  private val requestR = """(\S+) (\S+) (\S+)""".r // Start of HTTP request
  private val acceptR = """Accept: (.+)""".r // Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
  private val acceptEncR = """Accept-Encoding: (.+)""".r // Accept-Encoding: gzip, deflate
  private val acceptLangR = """Accept-Language: (.+)""".r // Accept-Language: en-GB,en;q=0.5
  private val connectionR = """Connection: (\S+)""".r // Connection: keep-alive
  private val dntR = """DNT: (\d)""".r // DNT: 1
  private val hostR = """Host: (\S+)""".r // Host: www.doc.ic.ac.uk
  private val upgradeirR = """Upgrade-Insecure-Requests: (\d)""".r // Upgrade-Insecure-Requests: 1
  private val useragentR = """User-Agent: (.+)""".r // User-Agent: Mozilla/5.0 (Windows NT 6.3; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0
  
  private val genericHeaderR = """(\S+): (.+)""".r // Generic regex for unsupported headers
  
  override def destreamer(): Any = {
    import java.net.URI
    
    val line = inb.readLine()
    
    if (!requestStarted) {
      line match {
        case requestR(method, uri, version) => {
          requestStarted = true;
          val m = Method(method)
          val path = new URI(uri).getPath
          val v = Version(version)
          return Request(RequestLine(m, path, v))(SocketIn[InternalChoice1](this))
        }
        
        case null => {
          close()
          throw ConnectionClosed("Connection closed by client")
        }
        case e => {
          close()
          throw new ProtocolError(f"Unexpected initial message: '${e}'")
        }
      }
    }
    
    // If we are here, then requestStarted was false
    line match {
      case acceptR(fmts) => Accept(fmts)(SocketIn[InternalChoice1](this))
      case acceptEncR(encs) => AcceptEncodings(encs)(SocketIn[InternalChoice1](this))
      case acceptLangR(langs) => AcceptLanguage(langs)(SocketIn[InternalChoice1](this))
      case connectionR(conn) => Connection(conn)(SocketIn[InternalChoice1](this))
      case dntR(dnt) => DoNotTrack(dnt == 1)(SocketIn[InternalChoice1](this))
      case hostR(host) => Host(host)(SocketIn[InternalChoice1](this))
      case upgradeirR(up) => UpgradeIR(up == 1)(SocketIn[InternalChoice1](this))
      case useragentR(ua) => UserAgent(ua)(SocketIn[InternalChoice1](this))
      
      case genericHeaderR(h, _) if relaxHeaders => {
        // Ignore this header, and keep looking for something supported
        logger(f"Skipping unsupported HTTP header '${h}'")
        destreamer()
      }
      
      case "" => {
        // The request body should now follow.
        // TODO: in this HTTP fragment, we assume GET with Content-Length=0
        RequestBody(Body("text/html", Array[Byte]()))(SocketOut[HttpVersion](this))
      }
      
      case e => { close(); throw new ProtocolError(f"Unexpected message: '${e}'") }
    }
  }
}

