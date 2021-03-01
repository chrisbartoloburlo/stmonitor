package examples.game
import lchannels.{In, Out}
import monitor.util.ConnectionManager
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}
class Mon(external: ConnectionManager, internal: In[InternalChoice1], max: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Guess_3 {
			var num: Int = _
		}
		object Correct_1 {
			var ans: Int = _
		}
		object Incorrect_2 {
		}
		object Quit_4 {
		}
	}
	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		sendInternalChoice1(internal, external, 0).result
    external.close()
  }
	def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Guess(_) =>
				if(msg.num > 0 && msg.num < 10){
					external.send(msg)
					payloads.Guess_3.num = msg.num
					if (count < max) {
						receiveExternalChoice1(msg.cont, external, count+1)
					} else { tailcall(receiveExternalChoice1(msg.cont, external, 0)) }
				} else {
				done() }
			case msg @ Quit() =>
				external.send(msg)
				done()
			case _ => done()
		}
	}
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Correct(_)=>
				if(msg.ans==payloads.Guess_3.num){
					internal ! msg; done()
				} else {
				done() }
			case msg @ Incorrect()=>
				val cont = internal !! Incorrect()_
				if (count < max) {
					sendInternalChoice1(cont, external, count+1)
				} else { tailcall(sendInternalChoice1(cont, external,0)) }
			case _ => done()
		}
	}
}