package benchmarks.http.monitored

import lchannels.{In, Out}

import java.time.ZonedDateTime

case class Request(msg: RequestLine)(val cont: In[ExternalChoice1])
sealed abstract class ExternalChoice1
case class AcceptEncodings(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Accept(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class DoNotTrack(msg: Boolean)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class UpgradeIR(msg: Boolean)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Connection(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class UserAgent(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class AcceptLanguage(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Host(msg: String)(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class RequestBody(msg: Body)(val cont: Out[HttpVersion]) extends ExternalChoice1
case class HttpVersion(msg: Version)(val cont: In[InternalChoice3])
sealed abstract class InternalChoice3
case class Code404(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice3
sealed abstract class InternalChoice1
case class ETag_9(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Server_10(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class ContentLength_11(msg: Int)(val cont: In[InternalChoice1]) extends InternalChoice1
case class ContentType_12(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Vary_13(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Via_14(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class StrictTS_15(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class ResponseBody_16(msg: Body) extends InternalChoice1
case class AcceptRanges_17(msg: String)(val cont: In[InternalChoice1]) extends InternalChoice1
case class LastModified_18(msg: ZonedDateTime)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Date_19(msg: ZonedDateTime)(val cont: In[InternalChoice1]) extends InternalChoice1
case class Code200(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice3
sealed abstract class InternalChoice2
case class ETag_21(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice2
case class Server_22(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice2
case class ContentLength_23(msg: Int)(val cont: In[InternalChoice2]) extends InternalChoice2
case class ContentType_24(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice2
case class Vary_25(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice2
case class Via_26(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice2
case class StrictTS_27(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice2
case class ResponseBody_28(msg: Body) extends InternalChoice2
case class AcceptRanges_29(msg: String)(val cont: In[InternalChoice2]) extends InternalChoice2
case class LastModified_30(msg: ZonedDateTime)(val cont: In[InternalChoice2]) extends InternalChoice2
case class Date_31(msg: ZonedDateTime)(val cont: In[InternalChoice2]) extends InternalChoice2
