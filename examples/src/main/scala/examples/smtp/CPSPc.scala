package examples.smtp

import lchannels.Out
case class M220(msg: String)(val cont: Out[InternalChoice3])
sealed abstract class InternalChoice3
case class Helo(hostname: String)(val cont: Out[M250_13]) extends InternalChoice3
case class M250_13(msg: String)(val cont: Out[InternalChoice2])
sealed abstract class InternalChoice2
case class MailFrom(addr: String)(val cont: Out[M250_9]) extends InternalChoice2
case class M250_9(msg: String)(val cont: Out[InternalChoice1])
sealed abstract class InternalChoice1
case class RcptTo(addr: String)(val cont: Out[M250_1]) extends InternalChoice1
case class M250_1(msg: String)(val cont: Out[InternalChoice1])
case class Data()(val cont: Out[M354]) extends InternalChoice1
case class M354(msg: String)(val cont: Out[Content])
case class Content(txt: String)(val cont: Out[M250_3])
case class M250_3(msg: String)(val cont: Out[InternalChoice2])
case class Quit_8()(val cont: Out[M221_7]) extends InternalChoice1
case class M221_7(msg: String)
case class Quit_12()(val cont: Out[M221_11]) extends InternalChoice2
case class M221_11(msg: String)
case class Quit_16()(val cont: Out[M221_15]) extends InternalChoice3
case class M221_15(msg: String)
