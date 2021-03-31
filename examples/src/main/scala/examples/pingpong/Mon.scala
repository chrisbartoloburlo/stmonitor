package examples.pingpong

import lchannels.{In, Out}
import monitor.util.ConnectionManager
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}
class Mon(external: ConnectionManager, internal: Out[ExternalChoice1], max: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Ping_2 {
		}
		object Pong_1 {
		}
		object Quit_3 {
		}
	}
	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		receiveExternalChoice1(internal, external, 0).result
    external.close()
  }
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Ping()=>
				val cont = internal !! Ping()_
				if (count < max) {
					sendPong_1(cont, external, count+1)
				} else { tailcall(sendPong_1(cont, external, 0)) }
			case msg @ Quit()=>
				internal ! msg; done()
			case _ => done()
		}
	}
	def sendPong_1(internal: In[Pong], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Pong() =>
				external.send(msg)
				if (count < max) {
					receiveExternalChoice1(msg.cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(msg.cont, external, 0)) }
			case _ => done()
		}
	}
}