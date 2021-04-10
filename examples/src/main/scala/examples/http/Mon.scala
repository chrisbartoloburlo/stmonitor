package examples.http
import lchannels.{In, Out}

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}

class Mon(external: ConnectionManager, internal: Out[Request], max: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Request_35 {
			var msg: RequestLine = _
		}
		object AcceptEncodings_1 {
			var msg: String = _
		}
		object Accept_2 {
			var msg: String = _
		}
		object DoNotTrack_3 {
			var msg: Boolean = _
		}
		object UpgradeIR_4 {
			var msg: Boolean = _
		}
		object Connection_5 {
			var msg: String = _
		}
		object UserAgent_6 {
			var msg: String = _
		}
		object AcceptLanguage_7 {
			var msg: String = _
		}
		object Host_8 {
			var msg: String = _
		}
		object RequestBody_34 {
			var msg: Body = _
		}
		object HttpVersion_33 {
			var msg: Version = _
		}
		object Code404_20 {
			var msg: String = _
		}
		object ETag_9 {
			var msg: String = _
		}
		object Server_10 {
			var msg: String = _
		}
		object ContentLength_11 {
			var msg: Int = _
		}
		object ContentType_12 {
			var msg: String = _
		}
		object Vary_13 {
			var msg: String = _
		}
		object Via_14 {
			var msg: String = _
		}
		object StrictTS_15 {
			var msg: String = _
		}
		object ResponseBody_16 {
			var msg: Body = _
		}
		object AcceptRanges_17 {
			var msg: String = _
		}
		object LastModified_18 {
			var msg: ZonedDateTime = _
		}
		object Date_19 {
			var msg: ZonedDateTime = _
		}
		object Code200_32 {
			var msg: String = _
		}
		object ETag_21 {
			var msg: String = _
		}
		object Server_22 {
			var msg: String = _
		}
		object ContentLength_23 {
			var msg: Int = _
		}
		object ContentType_24 {
			var msg: String = _
		}
		object Vary_25 {
			var msg: String = _
		}
		object Via_26 {
			var msg: String = _
		}
		object StrictTS_27 {
			var msg: String = _
		}
		object ResponseBody_28 {
			var msg: Body = _
		}
		object AcceptRanges_29 {
			var msg: String = _
		}
		object LastModified_30 {
			var msg: ZonedDateTime = _
		}
		object Date_31 {
			var msg: ZonedDateTime = _
		}
	}
	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		receiveRequest_35(internal, external, 0).result
    external.close()
  }
  def receiveRequest_35(internal: Out[Request], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Request(_)=>
				val cont = internal !! Request(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case _ => done()
		}
	}
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ AcceptEncodings(_)=>
				val cont = internal !! AcceptEncodings(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ Accept(_)=>
				val cont = internal !! Accept(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ DoNotTrack(_)=>
				val cont = internal !! DoNotTrack(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ UpgradeIR(_)=>
				val cont = internal !! UpgradeIR(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ Connection(_)=>
				val cont = internal !! Connection(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ UserAgent(_)=>
				val cont = internal !! UserAgent(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ AcceptLanguage(_)=>
				val cont = internal !! AcceptLanguage(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ Host(_)=>
				val cont = internal !! Host(msg.msg)_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ RequestBody(_)=>
				val cont = internal !! RequestBody(msg.msg)_
				if (count < max) {
					sendHttpVersion_33(cont, external, count+1)
				} else { tailcall(sendHttpVersion_33(cont, external, 0)) }
			case _ => done()
		}
	}
	def sendHttpVersion_33(internal: In[HttpVersion], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ HttpVersion(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice3(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice3(msg.cont, external, 0)) }
			case _ => done()
		}
	}
	def sendInternalChoice3(internal: In[InternalChoice3], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Code404(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ Code200(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case _ => done()
		}
	}
	def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ ETag(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ Server(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ ContentLength(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ ContentType(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ Vary(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ Via(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ StrictTS(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ ResponseBody(_) =>
				external.send(msg)
				done()
			case msg @ AcceptRanges(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ LastModified(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case msg @ Date(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice1(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice1(msg.cont, external, 0)) }
			case _ => done()
		}
	}
	def sendInternalChoice2(internal: In[InternalChoice2], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ ETag2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ Server2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ ContentLength2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ ContentType2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ Vary2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ Via2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ StrictTS2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ ResponseBody2(_) =>
				external.send(msg)
				done()
			case msg @ AcceptRanges2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ LastModified2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case msg @ Date2(_) =>
				external.send(msg)
				if (count < max) {
					sendInternalChoice2(msg.cont, external, count+1)
				} else { tailcall(sendInternalChoice2(msg.cont, external, 0)) }
			case _ => done()
		}
	}
}