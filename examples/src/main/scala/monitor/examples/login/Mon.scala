package monitor.examples.login
import akka.actor._
import lchannels.{In, Out}
import monitor.examples.login.tcp.SynConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
class Mon(Internal: Out[Login])(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
  object payloads {
		object Login {
			var uname: String = _
			var pwd: String = _
			var token: String = _
		}
		object Success {
			var id: String = _
		}
		object Retry {
		}
	}
  def receive: Receive = {
    case MonStart =>
      println("[Mon] Monitor started")
      println("[Mon] Setting up connection manager")
      val cm = new SynConnectionManager()
      cm.setup()
      receiveLogin(Internal, cm)
      cm.close()
  }
  def receiveLogin(internal: Out[Login], External: SynConnectionManager): Any = {
    External.receive() match {
      case msg @ Login(_, _, _)=>
        if(util.validateAuth(msg.uname, msg.token)){
          val cont = internal !! Login(msg.uname, msg.pwd, msg.token)_
					payloads.Login.uname = msg.uname
					sendInternalChoice1(cont, External)
        } else {
          External.close()
          throw new Exception("[Mon] Incorrect value received by client")
        }
      case _ =>
        External.close()
        throw new Exception("[Mon] Received unknown message from client")
    }
  }
  def sendInternalChoice1(internal: In[InternalChoice1], External: SynConnectionManager): Any = {
    internal ? {
      case msg @ Success(_) =>
        if(util.validateId(msg.id,payloads.Login.uname)){
          External.send(msg)
        } else {
          External.close()
          throw new Exception("[Mon] Incorrect value sent by server")
        }
      case msg @ Retry() =>
        External.send(msg)
				receiveLogin(msg.cont, External)
    }
  }
}