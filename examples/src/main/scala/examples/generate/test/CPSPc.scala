package examples.generate.test
import lchannels.{In, Out}
case class L1()(val cont: Out[L2])
case class L2()(val cont: Out[L1])
