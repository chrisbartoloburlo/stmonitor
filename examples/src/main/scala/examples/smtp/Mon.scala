package examples.smtp

import lchannels.{In, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}

class Mon(external: ConnectionManager, internal: Out[M220], max: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object M220_17 {
			var msg: String = _
		}
		object Helo_14 {
			var hostname: String = _
		}
		object M250_13 {
			var msg: String = _
		}
		object MailFrom_10 {
			var addr: String = _
		}
		object M250_9 {
			var msg: String = _
		}
		object RcptTo_2 {
			var addr: String = _
		}
		object M250_1 {
			var msg: String = _
		}
		object Data_6 {
		}
		object M354_5 {
			var msg: String = _
		}
		object Content_4 {
			var txt: String = _
		}
		object M250_3 {
			var msg: String = _
		}
		object Quit_8 {
		}
		object M221_7 {
			var msg: String = _
		}
		object Quit_12 {
		}
		object M221_11 {
			var msg: String = _
		}
		object Quit_16 {
		}
		object M221_15 {
			var msg: String = _
		}
	}
	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		receiveM220_17(internal, external, 0).result
    external.close()
  }
  def receiveM220_17(internal: Out[M220], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M220(_)=>
				val cont = internal !! M220(msg.msg)_
				if (count < max) {
					sendExternalChoice3(cont, external, count+1)
				} else { tailcall(sendExternalChoice3(cont, external,0)) }
			case _ => done()
		}
	}
	def sendExternalChoice3(internal: In[ExternalChoice3], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Helo(_) =>
				external.send(msg)
				if (count < max) {
					receiveM250_13(msg.cont, external, count+1)
				} else { tailcall(receiveM250_13(msg.cont, external, 0)) }
			case msg @ Quit_16() =>
				external.send(msg)
				if (count < max) {
					receiveM221_15(msg.cont, external, count+1)
				} else { tailcall(receiveM221_15(msg.cont, external, 0)) }
			case _ => done()
		}
	}
  def receiveM250_13(internal: Out[M250_13], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M250_13(_)=>
				val cont = internal !! M250_13(msg.msg)_
				if (count < max) {
					sendExternalChoice2(cont, external, count+1)
				} else { tailcall(sendExternalChoice2(cont, external,0)) }
			case _ => done()
		}
	}
	def sendExternalChoice2(internal: In[ExternalChoice2], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ MailFrom(_) =>
				external.send(msg)
				if (count < max) {
					receiveM250_9(msg.cont, external, count+1)
				} else { tailcall(receiveM250_9(msg.cont, external, 0)) }
			case msg @ Quit_12() =>
				external.send(msg)
				if (count < max) {
					receiveM221_11(msg.cont, external, count+1)
				} else { tailcall(receiveM221_11(msg.cont, external, 0)) }
			case _ => done()
		}
	}
  def receiveM250_9(internal: Out[M250_9], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M250_9(_)=>
				val cont = internal !! M250_9(msg.msg)_
				if (count < max) {
					sendExternalChoice1(cont, external, count+1)
				} else { tailcall(sendExternalChoice1(cont, external,0)) }
			case _ => done()
		}
	}
	def sendExternalChoice1(internal: In[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ RcptTo(_) =>
				external.send(msg)
				if (count < max) {
					receiveM250_1(msg.cont, external, count+1)
				} else { tailcall(receiveM250_1(msg.cont, external, 0)) }
			case msg @ Data() =>
				external.send(msg)
				if (count < max) {
					receiveM354_5(msg.cont, external, count+1)
				} else { tailcall(receiveM354_5(msg.cont, external, 0)) }
			case msg @ Quit_8() =>
				external.send(msg)
				if (count < max) {
					receiveM221_7(msg.cont, external, count+1)
				} else { tailcall(receiveM221_7(msg.cont, external, 0)) }
			case _ => done()
		}
	}
  def receiveM250_1(internal: Out[M250_1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M250_1(_)=>
				val cont = internal !! M250_1(msg.msg)_
				if (count < max) {
					sendExternalChoice1(cont, external, count+1)
				} else { tailcall(sendExternalChoice1(cont, external,0)) }
			case _ => done()
		}
	}
  def receiveM354_5(internal: Out[M354], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M354(_)=>
				val cont = internal !! M354(msg.msg)_
				if (count < max) {
					sendContent_4(cont, external, count+1)
				} else { tailcall(sendContent_4(cont, external, 0)) }
			case _ => done()
		}
	}
	def sendContent_4(internal: In[Content], external: ConnectionManager, count: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Content(_) =>
				external.send(msg)
				if (count < max) {
					receiveM250_3(msg.cont, external, count+1)
				} else { tailcall(receiveM250_3(msg.cont, external, 0)) }
			case _ => done()
		}
	}
  def receiveM250_3(internal: Out[M250_3], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M250_3(_)=>
				val cont = internal !! M250_3(msg.msg)_
				if (count < max) {
					sendExternalChoice2(cont, external, count+1)
				} else { tailcall(sendExternalChoice2(cont, external,0)) }
			case _ => done()
		}
	}
  def receiveM221_7(internal: Out[M221_7], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M221_7(_)=>
				internal ! msg; done()
			case _ => done()
		}
	}
  def receiveM221_11(internal: Out[M221_11], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M221_11(_)=>
				internal ! msg; done()
			case _ => done()
		}
	}
  def receiveM221_15(internal: Out[M221_15], external: ConnectionManager, count: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ M221_15(_)=>
				internal ! msg; done()
			case _ => done()
		}
	}
}