package monitor.examples.login

import akka.actor._
import lchannels.{In, Out}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.collection.mutable
class Mon(Internal: Out[Login])(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
  private var value = new mutable.HashMap[(String, String), Any]
  def receive: Receive = {
    case MonStart =>
      println("[Mon] Monitor started")
      println("[Mon] Setting up connection manager")
      val cm = new ConnectionManager()
      cm.setup()
      receiveLogin(Internal, cm)
      cm.close()
  }
  def receiveLogin(internal: Out[Login], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ Login(_, _, _)=>
        if(util.validateAuth(msg.uname, msg.token)){
          val cont = internal !! Login(msg.uname, msg.pwd, msg.token)_
					value(("Login", "uname")) = msg.uname
					sendInternalChoice1(cont, External)
        } else {
        }
    }
  }
  def sendInternalChoice1(internal: In[InternalChoice1], External: ConnectionManager): Any = {
    internal ? {
      case msg @ Success(_) =>
        if(util.validateId(msg.id,value("Login", "uname").asInstanceOf[String])){
          External.send(msg)
        } else {
        }
      case msg @ Retry() =>
        External.send(msg)
				receiveLogin(msg.cont, External)
    }
  }
}