package examples.game
import lchannels.{In, Out}
sealed abstract class InternalChoice1
case class Guess(num: Int)(val cont: Out[ExternalChoice1]) extends InternalChoice1
sealed abstract class ExternalChoice1
case class Correct(ans: Int) extends ExternalChoice1
case class Incorrect()(val cont: Out[InternalChoice1]) extends ExternalChoice1
case class Quit() extends InternalChoice1
