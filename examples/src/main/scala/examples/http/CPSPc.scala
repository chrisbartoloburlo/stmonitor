package examples.http
import java.time.ZonedDateTime
import lchannels.{In, Out}
case class Request(msg: RequestLine)(val cont: In[InternalChoice1])
sealed abstract class InternalChoice1
case class AcceptEncodings(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Accept(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class DoNotTrack(msg: Boolean)(val cont: In[InternalChoice1]) extends InternalChoice1
case class UpgradeIR(msg: Boolean)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Connection(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class UserAgent(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class AcceptLanguage(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Host(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class RequestBody(msg: Body)(val cont: Out[HttpVersion]) extends InternalChoice1
case class HttpVersion(msg: Version)(val cont: In[ExternalChoice3])
sealed abstract class ExternalChoice3
case class Code404(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice3
sealed abstract class ExternalChoice1
case class ETag(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Server(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class ContentLength(msg: Int)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class ContentType(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Vary(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Via(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class StrictTS(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class ResponseBody(msg: Body) extends ExternalChoice1
case class AcceptRanges(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class LastModified(msg: ZonedDateTime)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Date(msg: ZonedDateTime)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Code200(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice3
sealed abstract class ExternalChoice2
case class ETag2(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class Server2(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class ContentLength2(msg: Int)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class ContentType2(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class Vary2(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class Via2(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class StrictTS2(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class ResponseBody2(msg: Body) extends ExternalChoice2
case class AcceptRanges2(msg: String)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class LastModified2(msg: ZonedDateTime)(val cont: In[ExternalChoice2]) extends ExternalChoice2
case class Date2(msg: ZonedDateTime)(val cont: In[ExternalChoice2]) extends ExternalChoice2
