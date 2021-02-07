package examples.execute.coin

import lchannels.In
sealed abstract class ExternalChoice1
case class Heads()(val cont: In[ExternalChoice1]) extends ExternalChoice1
case class Tails()(val cont: In[ExternalChoice1]) extends ExternalChoice1
