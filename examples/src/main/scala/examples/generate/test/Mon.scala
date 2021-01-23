package examples.generate.test
import lchannels.{In, Out}
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
class Mon(external: ConnectionManager, internal: In[L1])(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
  object payloads {
		object L1_2 {
		}
		object L2_1 {
		}
	}
  override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
    external.setup()
    var control: (String, Any) = sendL1_2(internal)
    while(true){
      control match {
        case c @ ("receiveL2_1", _) =>
          control = receiveL2_1(c._2.asInstanceOf[Out[L2]])
        case c @ ("sendL1_2", _) =>
          control = sendL1_2(c._2.asInstanceOf[In[L1]])
        case("err", _) =>
          
      }
    }
    external.close()
  }
  def sendL1_2(internal: In[L1]): (String, Out[L2]) = {
    internal ? {
      case msg @ L1() =>
        external.send(msg)
        ("receiveL2_1", msg.cont)
//        receiveL2_1(msg.cont, external)
    }
  }
  def receiveL2_1(internal: Out[L2]): (String, In[L1]) = {
    external.receive() match {
      case msg @ L2() =>
				val cont = internal !! L2()_
        ("sendL1_2", cont)
//				sendL1_2(cont)
      case _ =>
        ("err", null)
    }
  }
}