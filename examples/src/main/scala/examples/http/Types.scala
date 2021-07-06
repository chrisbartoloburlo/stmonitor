package examples.http

sealed abstract class Version
case object Http11 extends Version {
  override def toString() = "HTTP/1.1"
}

object Version {
  /** Construct Version object from a string, as found in an HTTP request. */
  def apply(v: String) = v match {
    case "HTTP/1.1" => Http11
    case _ => throw new RuntimeException(f"Unsupported HTTP version: ${v}")
  }
}

sealed abstract class Method
case object GET extends Method {
  override def toString() = "GET"
}

object Method {
  /** Construct method object from a string, as found in an HTTP request. */
  def apply(v: String) = v match {
    case "GET" => GET
    case _ => throw new RuntimeException(f"Unsupported method: ${v}")
  }
}

case class RequestLine(method: Method, path: String, version: Version)

case class Body(contentType: String, contents: Array[Byte])
