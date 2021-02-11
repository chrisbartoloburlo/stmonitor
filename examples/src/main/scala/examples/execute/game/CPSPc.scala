package examples.execute.game

import lchannels.{In, Out}
sealed abstract class ExternalChoice1
case class Guess(num: Int)(val cont: Out[InternalChoice1]) extends ExternalChoice1
sealed abstract class InternalChoice1
case class Correct()(val cont: Out[ExternalChoice1]) extends InternalChoice1
case class Incorrect()(val cont: Out[ExternalChoice1]) extends InternalChoice1
case class New()(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Quit() extends ExternalChoice1
