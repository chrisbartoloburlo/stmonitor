package benchmarks.smtp.lchannels

import lchannels.Out

case class M220(msg: String)(val cont: Out[InternalChoice3])
sealed abstract class InternalChoice3
case class Helo(hostname: String)(val cont: Out[M250_1]) extends InternalChoice3
case class M250_1(msg: String)(val cont: Out[InternalChoice2])
sealed abstract class InternalChoice2
case class MailFrom(addr: String)(val cont: Out[M250_2]) extends InternalChoice2
case class M250_2(msg: String)(val cont: Out[InternalChoice1])
sealed abstract class InternalChoice1
case class RcptTo(addr: String)(val cont: Out[M250_3]) extends InternalChoice1
case class M250_3(msg: String)(val cont: Out[InternalChoice1])
case class Data()(val cont: Out[M354]) extends InternalChoice1
case class M354(msg: String)(val cont: Out[Content])
case class Content(txt: String)(val cont: Out[M250_4])
case class M250_4(msg: String)(val cont: Out[InternalChoice2])
case class Quit_1()(val cont: Out[M221_1]) extends InternalChoice1
case class M221_1(msg: String)
case class Quit_2()(val cont: Out[M221_2]) extends InternalChoice2
case class M221_2(msg: String)
case class Quit_3()(val cont: Out[M221_3]) extends InternalChoice3
case class M221_3(msg: String)
case class MonStart()
