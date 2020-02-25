package monitor.examples.login
import lchannels.{In, Out}
case class Login(uname: String, pwd: String, token: String)(val cont: Out[InternalChoice1])
sealed abstract class InternalChoice1
case class Success(id: String) extends InternalChoice1
case class Retry()(val cont: Out[Login]) extends InternalChoice1
case class MonStart()
