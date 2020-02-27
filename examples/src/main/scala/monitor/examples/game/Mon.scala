package monitor.examples.game

import akka.actor._
import com.typesafe.scalalogging.Logger
import lchannels.{In, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class Mon(Internal: In[InternalChoice1])(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
  val logger: Logger = Logger("Mon")
  object payloads {
		object Guess {
			var num: Int = _
		}
		object Correct {
			var ans: Int = _
		}
		object Incorrect {
		}
		object Quit {
		}
	}
  def receive: Receive = {
    case MonStart =>
      logger.info("Monitor started")
      logger.info("Setting up connection manager")
      val cm = new ConnectionManager()
      cm.setup()
      sendInternalChoice1(Internal, cm)
      cm.close()
  }
  def sendInternalChoice1(internal: In[InternalChoice1], External: ConnectionManager): Any = {
    internal ? {
      case msg @ Guess(_) =>
        if(msg.num > 0 && msg.num < 10){
          External.send(msg)
					payloads.Guess.num = msg.num
					receiveExternalChoice1(msg.cont, External)
        } else {
          External.close()
          throw new Exception("[Mon] Incorrect value sent by client")
        }
      case msg @ Quit() =>
        External.send(msg)
    }
  }
  def receiveExternalChoice1(internal: Out[ExternalChoice1], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ Correct(_)=>
        if(msg.ans==payloads.Guess.num){
          	internal ! msg
        } else {
          External.close()
          throw new Exception("[Mon] Incorrect value sent by server")
        }
      case msg @ Incorrect()=>
        val cont = internal !! Incorrect()_
				sendInternalChoice1(cont, External)
      case _ =>
        External.close()
        throw new Exception("[Mon] Received unknown message from server")
    }
  }
}