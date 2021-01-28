package examples.execute.pingpong

//import akka.actor._ FIXME REMOVE
import lchannels.{In, Out}
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

//abstract class Control
//case class Continue(f: () => Control) extends Control
//case class Stop() extends Control
import scala.util.control.TailCalls.{TailRec, done, tailcall}

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
    receiveExternalChoice1(Internal, External).result
//    var control: Control = Continue( () => receiveExternalChoice1(Internal, External))
//    var quit = false
//    while(!quit){
//      control match {
//        case Continue(f) =>
//          control = f.apply()
//        case Stop() =>
//          quit = true
//      }
//    }
    External.close()
  }
  def receiveExternalChoice1(internal: Out[ExternalChoice1], External: ConnectionManager): TailRec[Unit] = {
    External.receive() match {
      case msg @ Ping()=>
        println("[Mon] Received Ping")
        val cont = internal !! Ping()_
//        Continue(() => sendPong(cont, External))
        tailcall(sendPong(cont, External))
      case msg @ Quit()=>
        internal ! msg
//        Stop()
        done()
      case _ =>
//        Stop()
        done()
    }
  }
  def sendPong(internal: In[Pong], External: ConnectionManager): TailRec[Unit] = {
    println("[Mon] in sendPong")
    internal ? {
      case msg @ Pong() =>
        println("[Mon] Sending Pong")
        External.send(msg)
//        Continue( () => receiveExternalChoice1(msg.cont, External) )
        tailcall(receiveExternalChoice1(msg.cont, External))
    }
  }
}