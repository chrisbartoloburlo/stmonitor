package benchmarks.pingpong

import lchannels.{In, Out}

case class Ping()(val cont: Out[Pong])
case class Pong()(val cont: Out[Ping])

case class MonStart()
