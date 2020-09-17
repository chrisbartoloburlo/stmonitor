package monitor.examples.auth
import lchannels.{In, Out}
case class Auth(uname: String, pwd: String)(val cont: Out[InternalChoice1])
sealed abstract class InternalChoice1
case class Succ(tok: String) extends InternalChoice1
case class Fail(Code: Int)(val cont: Out[Auth]) extends InternalChoice1
case class MonStart()
