package examples.execute.pingpong

//import akka.actor._ FIXME REMOVE
import lchannels.{In, Out}
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class Mon(External: ConnectionManager, Internal: Any)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
  object payloads {
		object Ping {
		}
		object Pong {
		}
		object Quit {
		}
	}
  override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
    External.setup()
    receiveExternalChoice1(Internal.asInstanceOf[Out[ExternalChoice1]], External)
    External.close()
  }
//  def receive: Receive = { FIXME REMOVE
//    case MonStart =>
//      println("[Mon] Monitor started")
//      println("[Mon] Setting up connection manager")
//      val cm = new ConnectionManager()
//      cm.setup()
//      receiveExternalChoice1(Internal, cm)
//      cm.close()
//  }
  def receiveExternalChoice1(internal: Out[ExternalChoice1], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ Ping()=>
        val cont = internal !! Ping()_
        sendPong(cont, External)
      case msg @ Quit()=>
        internal ! msg
      case _ =>
    }
  }
  def sendPong(internal: In[Pong], External: ConnectionManager): Any = {
    internal ? {
      case msg @ Pong() =>
        External.send(msg)
        receiveExternalChoice1(msg.cont, External)
    }
  }
}