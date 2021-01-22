package examples.execute.pingpong

//import akka.actor._ FIXME REMOVE
import lchannels.{In, Out}
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

abstract class Control
case class Continue(f: () => Control) extends Control
case class Stop() extends Control

class Mon(External: ConnectionManager, Internal: Out[ExternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
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
    var control: Control = Continue( () => receiveExternalChoice1(Internal, External))
    var quit = false
    while(!quit){
      control match {
        case Continue(f) =>
          f.apply()
        case Stop() =>
          quit = true
      }
    }
    External.close()
  }
  def receiveExternalChoice1(internal: Out[ExternalChoice1], External: ConnectionManager): Control = {
    External.receive() match {
      case msg @ Ping()=>
        val cont = internal !! Ping()_
        Continue( () => sendPong(cont, External) )
//        sendPong(cont, External)
      case msg @ Quit()=>
        internal ! msg
        Stop()
      case _ =>
        Stop()
    }
  }
  def sendPong(internal: In[Pong], External: ConnectionManager): Control = {
    internal ? {
      case msg @ Pong() =>
        External.send(msg)
        Continue( () => receiveExternalChoice1(msg.cont, External) )
//        receiveExternalChoice1(msg.cont, External)
    }
  }
}