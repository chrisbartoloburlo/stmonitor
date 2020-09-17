package monitor.examples.auth
import akka.actor._
import lchannels.{In, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
class Mon(Internal: Out[Auth])(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
  object payloads {
		object Auth {
			var uname: String = _
			var pwd: String = _
		}
		object Succ {
			var tok: String = _
		}
		object Fail {
			var Code: Int = _
		}
	}
  def receive: Receive = {
    case MonStart =>
      println("[Mon] Monitor started")
      println("[Mon] Setting up connection manager")
      val cm = new SynConnectionManager()
      cm.setup()
      receiveAuth(Internal, cm)
      cm.close()
  }
  def receiveAuth(internal: Out[Auth], External: SynConnectionManager): Any = {
    External.receive() match {
      case msg @ Auth(_, _)=>
        if(util.validateUname(msg.uname)){
          val cont = internal !! Auth(msg.uname, msg.pwd)_
					payloads.Auth.uname = msg.uname
					sendInternalChoice1(cont, External)
        } else {
        }
      case _ =>
    }
  }
  def sendInternalChoice1(internal: In[InternalChoice1], External: SynConnectionManager): Any = {
    internal ? {
      case msg @ Succ(_) =>
        if(util.validateTok(msg.tok, payloads.Auth.uname)){
          External.send(msg)
        } else {
        }
      case msg @ Fail(_) =>
        External.send(msg)
				receiveAuth(msg.cont, External)
    }
  }
}