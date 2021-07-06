package examples.http

import java.time.ZonedDateTime

import scala.concurrent.duration._

import lchannels.{In, Out}

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

case class MPRequestChoice(c: In[InternalChoice1]) {
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

case class MPCode200OrCode404(c: Out[ExternalChoice3]) {
  def send(v: MPCode200): MPResponseChoice200 = {
    val cnt = c !! Code200(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPCode404): MPResponseChoice404 = {
    val cnt = c !! Code404(v.p)_
    MPResponseChoice404(cnt)
  }
}

case class MPResponseChoice200(c: Out[ExternalChoice2]) {
  def send(v: MPAcceptRanges): MPResponseChoice200 = {
    val cnt = c !! AcceptRanges2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPContentLength): MPResponseChoice200 = {
    val cnt = c !! ContentLength2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPContentType): MPResponseChoice200 = {
    val cnt = c !! ContentType2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPDate): MPResponseChoice200 = {
    val cnt = c !! Date2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPETag): MPResponseChoice200 = {
    val cnt = c !! ETag2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPLastModified): MPResponseChoice200 = {
    val cnt = c !! LastModified2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPResponseBody): Unit = {
    c ! ResponseBody2(v.p)
  }
  def send(v: MPServer): MPResponseChoice200 = {
    val cnt = c !! Server2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPStrictTS): MPResponseChoice200 = {
    val cnt = c !! StrictTS2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPVary): MPResponseChoice200 = {
    val cnt = c !! Vary2(v.p)_
    MPResponseChoice200(cnt)
  }
  def send(v: MPVia): MPResponseChoice200 = {
    val cnt = c !! Via2(v.p)_
    MPResponseChoice200(cnt)
  }
}

case class MPResponseChoice404(c: Out[ExternalChoice1]) {
  def send(v: MPAcceptRanges): MPResponseChoice404 = {
    val cnt = c !! AcceptRanges(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPContentLength): MPResponseChoice404 = {
    val cnt = c !! ContentLength(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPContentType): MPResponseChoice404 = {
    val cnt = c !! ContentType(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPDate): MPResponseChoice404 = {
    val cnt = c !! Date(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPETag): MPResponseChoice404 = {
    val cnt = c !! ETag(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPLastModified): MPResponseChoice404 = {
    val cnt = c !! LastModified(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPResponseBody): Unit = {
    c ! ResponseBody(v.p)
  }
  def send(v: MPServer): MPResponseChoice404 = {
    val cnt = c !! Server(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPStrictTS): MPResponseChoice404 = {
    val cnt = c !! StrictTS(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPVary): MPResponseChoice404 = {
    val cnt = c !! Vary(v.p)_
    MPResponseChoice404(cnt)
  }
  def send(v: MPVia): MPResponseChoice404 = {
    val cnt = c !! Via(v.p)_
    MPResponseChoice404(cnt)
  }
}

