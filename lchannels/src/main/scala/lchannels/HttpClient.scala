// lchannels - session programming in Scala
// Copyright (c) 2021, Alceste Scalas
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
//
// * Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.
/** @author Alceste Scalas <alcsc@dtu.dk> */
package lchannels

import scala.concurrent.duration.Duration

import java.net.{Socket => JSocket}

import rawhttp.core.{RawHttp, RawHttpResponse}

/** Base class for HTTP session management and (de)serialization of messages.
 *  
 *  A "session" may span across multiple client-server connections, so the
 *  underlying socket may be updated as needed.
 */
abstract class HttpClientManager(hostname: String, port: Int) {
  private val http = new RawHttp()
  private var socket: Option[JSocket] = None

  /** Convert an object into an HTTP request and send it.
   *  
   *  @throws Exception if a serialization error occurs.
   */
  protected[lchannels] final def streamer(x: Any): Unit = {
    val s = new JSocket(hostname, port)
    val (req, respHandler) = request(x)
    http.parseRequest(req).writeTo(s.getOutputStream)
    respHandler match {
      case Some(h) => h(getResponse())
      case None => ()
    }
    this.socket = Some(s)
  }
  
  /** Get an HTTP response and turn it into a message object and return it.
   *  
   *  @param atMost Maximum wait time
   *  
   *  @throws java.util.concurrent.TimeoutException if after waiting for `atMost`, no message arrives
   *  @throws Exception if a deserialization error occurs.
   */
  protected[lchannels] final def destreamer(atMost: Duration): Any = {
    val s = this.socket.get // Assuming that streamer() has set the socket
    if (atMost.isFinite) {
      s.setSoTimeout(java.lang.Math.toIntExact(atMost.toMillis))
    }

    try {
      val resp = http.parseResponse(s.getInputStream()).eagerly()
      val ret = response(resp)
      this.finalize()
      ret
    } catch {
      case e: Exception => {
        val msg = e.getMessage()
        http.parseResponse("HTTP/1.1 500 Internal Server Error\n" +
                           "Content-Type: text/plain\n" +
                           f"Content-Length: ${msg.getBytes.size}\n\n" +
                           msg).writeTo(s.getOutputStream())
        this.finalize()
        throw e
      }
    }
  }

  /** Convert the given object into an HTTP request (as a string),
   *  with an optional function that handles the server response.
   *  
   *  @param x Object to convert
   */
  def request(x: Any): (String, Option[RawHttpResponse[_] => Unit])
  
  /** Convert the given HTTP response into an object
   *  
   *  @param r HTTP response to convert
   *  @throws Exception if the given response cannot be converted error occurs.
   */
  def response(r: RawHttpResponse[_]): Any
  
  /** Send a response and close the socket.
   * 
   *  @param resp The response to be sent.
   */
  def getResponse(): RawHttpResponse[_] = {
    val s = this.socket.get // Assume the socket was set by previous request
    val res = http.parseResponse(s.getInputStream())
    finalize()
    res
  }

  /** Finalize the HTTP manager, closing any open socket. */
  override def finalize() = this.socket match {
    case Some(s) => s.close(); this.socket = None
    case None => ()
  }
  
  /** Create a pair of I/O socket-based channel endpoints,
   *  reading from `in` and writing to `out`.
   *  
   *  @param ec Execution context for internal `Promise`/`Future` handling
   */
  def factory[T](): (HttpClientIn[T], HttpClientOut[T]) = {
    (HttpClientIn[T](this), HttpClientOut[T](this))
  }
}

/** HTTP-based input channel endpoint (client side), usually created
 *  through the [[[HttpClientIn$.apply* companion object]]]
 *  or via [[HttpClientManager.factory]].
 */
protected[lchannels] class HttpClientIn[T](hm: HttpClientManager)
    extends medium.In[Http, T] {
  override def receive() =  {
    hm.destreamer(Duration.Inf).asInstanceOf[T]
  }
  
  override def receive(implicit atMost: Duration) = {
    try {
      hm.destreamer(atMost).asInstanceOf[T]
    } catch {
      case e: java.net.SocketTimeoutException => {
        throw new java.util.concurrent.TimeoutException(e.getMessage())
      }
    }
  }
}

/** HTTP-based input channel endpoint (client side). */
object HttpClientIn {
  /** Return a HTTP-based input channel endpoint (client side)
   * 
   * @param hm HTTP client manager handling the session
   */
  def apply[T](hm: HttpClientManager) = {
    new HttpClientIn[T](hm)
  }
}

/** HTTP-based output channel endpoint (client side), usually created
 *  through the [[[HttpClientOut$.apply* companion object]]]
 *  or via [[HttpClientManager.factory]].
 */
class HttpClientOut[-T](hm: HttpClientManager)
    extends medium.Out[Http, T] {
  override def send(x: T) = hm.streamer(x)

  override def create[U]() = hm.factory()
}

/** Stream-based output channel endpoint. */
object HttpClientOut {
  /** Return a HTTP-based output channel endpoint (client side).
   * 
   * @param hm HTTP client manager handling the session
   */
  def apply[T](hm: HttpClientManager) = {
    new HttpClientOut[T](hm)
  }
}
