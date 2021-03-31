package examples.pingpong
import lchannels.{In, Out}
sealed abstract class ExternalChoice1
case class Ping()(val cont: Out[Pong]) extends ExternalChoice1
case class Pong()(val cont: Out[ExternalChoice1])
case class Quit() extends ExternalChoice1
