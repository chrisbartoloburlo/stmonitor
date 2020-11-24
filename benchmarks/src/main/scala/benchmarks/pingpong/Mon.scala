package benchmarks.pingpong

import akka.actor._
import lchannels.{In, Out}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class Mon(Internal: Out[Ping])(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
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
      receivePing(Internal, cm)
      cm.close()
  }
  def receivePing(internal: Out[Ping], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ Ping()=>
				val cont = internal !! Ping()_
				sendPong(cont, External)
      case _ =>
    }
  }
  def sendPong(internal: In[Pong], External: ConnectionManager): Any = {
    internal ? {
      case msg @ Pong() =>
        External.send(msg)
				receivePing(msg.cont, External)
    }
  }
}