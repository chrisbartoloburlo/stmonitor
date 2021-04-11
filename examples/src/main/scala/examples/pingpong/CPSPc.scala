package examples.pingpong
import lchannels.{In, Out}
sealed abstract class InternalChoice1
case class Ping()(val cont: Out[Pong]) extends InternalChoice1
case class Pong()(val cont: Out[InternalChoice1])
case class Quit() extends InternalChoice1
