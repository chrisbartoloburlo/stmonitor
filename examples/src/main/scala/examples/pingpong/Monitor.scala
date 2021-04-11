package examples.pingpong
import lchannels.{In, Out}
import monitor.util.ConnectionManager
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}
class Monitor(external: ConnectionManager, internal: Out[InternalChoice1], max: Int, report: String => Unit)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Ping_2 {
		}
		object Pong_1 {
		}
		object Quit_3 {
		}
	}
	override def run(): Unit = {
		report("[MONITOR] Monitor started, setting up connection manager")
		external.setup()
		receiveInternalChoice1(internal, external, 0).result
    external.close()
  }
	def receiveInternalChoice1(internal: Out[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Ping()=>
				val cont = internal !! Ping()_
				if (count < max) {
					sendPong_1(cont, external, count+1)
				} else { tailcall(sendPong_1(cont, external, 0)) }
			case msg @ Quit()=>
				internal ! msg; done()
			case msg @ _ => report(f"[MONITOR] VIOLATION unknown message: $msg"); done()
		}
	}
	def sendPong_1(internal: In[Pong], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Pong() =>
				external.send(msg)
				if (count < max) {
					receiveInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(receiveInternalChoice1(msg.cont, external, 0)) }
			case msg @ _ => report(f"[MONITOR] VIOLATION unknown message: $msg"); done()
		}
	}
}