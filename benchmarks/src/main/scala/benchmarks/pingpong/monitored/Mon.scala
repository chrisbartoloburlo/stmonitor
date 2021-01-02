package benchmarks.pingpong.monitored

import akka.actor._
import lchannels.{In, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class Mon(Internal: Out[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
  object payloads {
		object Ping {
		}
		object Pong {
		}
	}
  def receive: Receive = {
    case MonStart =>
      println("[Mon] Monitor started")
      println("[Mon] Setting up connection manager")
      val cm = new ConnectionManager()
      cm.setup()
      receiveExternalChoice1(Internal, cm)
      cm.close()
  }
  def receiveExternalChoice1(internal: Out[ExternalChoice1], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ Ping()=>
				val cont = internal !! Ping()_
				sendPong(cont, External)
      case msg @ Quit()=>
        internal ! Quit()
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