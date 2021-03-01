package examples.auth
import lchannels.{In, Out}
import monitor.util.ConnectionManager
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}
class Mon(external: ConnectionManager, internal: Out[Auth], max: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Auth_3 {
			var uname: String = _
			var pwd: String = _
		}
		object Succ_1 {
			var tok: String = _
		}
		object Fail_2 {
			var Code: Int = _
		}
	}
	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		receiveAuth_3(internal, external, 0).result
    external.close()
  }
  def receiveAuth_3(internal: Out[Auth], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Auth(_, _)=>
				if(util.validateUname(msg.uname)){
					val cont = internal !! Auth(msg.uname, msg.pwd)_
					payloads.Auth_3.uname = msg.uname
					if (count < max) {
						sendInternalChoice1(cont, external,count+1)
					} else { tailcall(sendInternalChoice1(cont, external,0)) }
				} else {
				done() }
			case _ => done()
		}
	}
	def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Succ(_) =>
				if(util.validateTok(msg.tok, payloads.Auth_3.uname)){
					external.send(msg)
					done()
				} else {
				done() }
			case msg @ Fail(_) =>
				external.send(msg)
				if (count < max) {
					receiveAuth_3(msg.cont, external, count+1)
				} else { tailcall(receiveAuth_3(msg.cont, external, 0)) }
		}
	}
}