package benchmarks.auth
import lchannels.{In, Out}
import monitor.util.ConnectionManager
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}
class Mon(external: ConnectionManager, internal: In[Auth], max: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Auth_7 {
			var uname: String = _
			var pwd: String = _
		}
		object Succ_5 {
			var tok: String = _
		}
		object Get_3 {
			var resource: String = _
			var tok: String = _
		}
		object Res_1 {
			var content: String = _
		}
		object Timeout_2 {
		}
		object Rvk_4 {
			var tok: String = _
		}
		object Fail_6 {
			var code: Int = _
		}
	}
	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		sendAuth_7(internal, external, 0).result
    external.close()
  }
	def sendAuth_7(internal: In[Auth], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Auth(_, _) =>
				external.send(msg)
				if (count < max) {
					receiveExternalChoice2(msg.cont, external, count+1)
				} else { tailcall(receiveExternalChoice2(msg.cont, external, 0)) }
			case _ => done()
		}
	}
	def receiveExternalChoice2(internal: Out[ExternalChoice2], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Succ(_)=>
				val cont = internal !! Succ(msg.tok)_
				if (count < max) {
					sendInternalChoice1(cont, external, count+1)
				} else { tailcall(sendInternalChoice1(cont, external,0)) }
			case msg @ Fail(_)=>
				internal ! msg; done()
			case _ => done()
		}
	}
	def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Get(_, _) =>
				external.send(msg)
				if (count < max) {
					receiveExternalChoice1(msg.cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(msg.cont, external, 0)) }
			case msg @ Rvk(_) =>
				external.send(msg)
				done()
			case _ => done()
		}
	}
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Res(_)=>
				val cont = internal !! Res(msg.content)_
				if (count < max) {
					sendInternalChoice1(cont, external, count+1)
				} else { tailcall(sendInternalChoice1(cont, external,0)) }
			case msg @ Timeout()=>
				val cont = internal !! Timeout()_
				if (count < max) {
					sendAuth_7(cont, external, count+1)
				} else { tailcall(sendAuth_7(cont, external, 0)) }
			case _ => done()
		}
	}
}