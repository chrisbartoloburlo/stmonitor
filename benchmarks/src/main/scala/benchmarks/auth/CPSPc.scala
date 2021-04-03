package benchmarks.auth
import lchannels.{In, Out}
case class Auth(uname: String, pwd: String)(val cont: Out[ExternalChoice2])
sealed abstract class ExternalChoice2
case class Succ(tok: String)(val cont: Out[InternalChoice1]) extends ExternalChoice2
sealed abstract class InternalChoice1
case class Get(resource: String, tok: String)(val cont: Out[ExternalChoice1]) extends InternalChoice1
sealed abstract class ExternalChoice1
case class Res(content: String)(val cont: Out[InternalChoice1]) extends ExternalChoice1
case class Timeout()(val cont: Out[Auth]) extends ExternalChoice1
case class Rvk(tok: String) extends InternalChoice1
case class Fail(code: Int) extends ExternalChoice2
