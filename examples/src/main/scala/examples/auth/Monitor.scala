package examples.auth
import lchannels.{In, Out}
import monitor.util.ConnectionManager
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}
class Monitor(external: ConnectionManager, internal: Out[Auth], max: Int, report: String => Unit)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Auth_7 {
			var uname: String = _
			var pwd: String = _
		}
		object Succ_5 {
			var origTok: String = _
		}
		object Get_3 {
			var resource: String = _
			var reqTok: String = _
		}
		object Res_1 {
			var content: String = _
		}
		object Timeout_2 {
		}
		object Rvk_4 {
			var rvkTok: String = _
		}
		object Fail_6 {
			var code: Int = _
		}
	}
	override def run(): Unit = {
		report("[MONITOR] Monitor started, setting up connection manager")
		external.setup()
		receiveAuth_7(internal, external, 0).result
    external.close()
  }
  def receiveAuth_7(internal: Out[Auth], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Auth(_, _)=>
				if(util.validateUname(msg.uname)){
					val cont = internal !! Auth(msg.uname, msg.pwd)_
					payloads.Auth_7.uname = msg.uname
					if (count < max) {
						sendExternalChoice2(cont, external,count+1)
					} else { tailcall(sendExternalChoice2(cont, external,0)) }
				} else {
				report("[MONITOR] VIOLATION in Assertion: util.validateUname(uname)"); done() }
			case msg @ _ => report(f"[MONITOR] VIOLATION unknown message: $msg"); done()
		}
	}
	def sendExternalChoice2(internal: In[ExternalChoice2], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Succ(_) =>
				if(util.validateTok(msg.origTok, payloads.Auth_7.uname)){
					external.send(msg)
					payloads.Succ_5.origTok = msg.origTok
					if (count < max) {
						receiveInternalChoice1(msg.cont, external, count+1)
					} else { tailcall(receiveInternalChoice1(msg.cont, external, 0)) }
				} else {
				report("[MONITOR] VIOLATION in Assertion: util.validateTok(origTok, uname)"); done() }
			case msg @ Fail(_) =>
				external.send(msg)
				done()
			case msg @ _ => report(f"[MONITOR] VIOLATION unknown message: $msg"); done()
		}
	}
	def receiveInternalChoice1(internal: Out[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Get(_, _)=>
				val cont = internal !! Get(msg.resource, msg.reqTok)_
						payloads.Get_3.reqTok = msg.reqTok
				if (count < max) {
					sendExternalChoice1(cont, external, count+1)
				} else { tailcall(sendExternalChoice1(cont, external,0)) }
			case msg @ Rvk(_)=>
				if(payloads.Succ_5.origTok==msg.rvkTok){
					internal ! msg; done()
				} else {
				report("[MONITOR] VIOLATION in Assertion: origTok==rvkTok"); done() }
			case msg @ _ => report(f"[MONITOR] VIOLATION unknown message: $msg"); done()
		}
	}
	def sendExternalChoice1(internal: In[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Res(_) =>
				if(payloads.Succ_5.origTok==payloads.Get_3.reqTok){
					external.send(msg)
					if (count < max) {
						receiveInternalChoice1(msg.cont, external, count+1)
					} else { tailcall(receiveInternalChoice1(msg.cont, external, 0)) }
				} else {
				report("[MONITOR] VIOLATION in Assertion: origTok==reqTok"); done() }
			case msg @ Timeout() =>
				external.send(msg)
				if (count < max) {
					receiveAuth_7(msg.cont, external, count+1)
				} else { tailcall(receiveAuth_7(msg.cont, external, 0)) }
			case msg @ _ => report(f"[MONITOR] VIOLATION unknown message: $msg"); done()
		}
	}
}