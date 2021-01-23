package newbenchmarks.smtp.monitored

import lchannels.{In, Out}
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

abstract class Control
case class Continue(f: () => Control) extends Control
case class Stop() extends Control

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
    var control: Control = Continue( () => receiveM220_17(internal, external, 0) )
    var quit = false
    while(!quit){
      control match {
        case Continue(f) =>
          control = f.apply()
        case Stop() =>
          quit = true
      }
    }
    external.close()
  }
  def receiveM220_17(internal: Out[M220], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M220(_)=>
				val cont = internal !! M220(msg.msg)_
        if (count < max) {
          sendInternalChoice3(cont, external, count+1)
        } else {
          Continue(() => sendInternalChoice3(cont, external, 0))
        }
      case _ =>
        Stop()
    }
  }
  def sendInternalChoice3(internal: In[InternalChoice3], external: ConnectionManager, count: Int): Control = {
    internal ? {
      case msg @ Helo(_) =>
        external.send(msg)
        if (count < max) {
          receiveM250_13(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM250_13(msg.cont, external, 0))
        }
      case msg @ Quit_16() =>
        external.send(msg)
        if (count < max) {
          receiveM221_15(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM221_15(msg.cont, external, 0))
        }
    }
  }
  def receiveM250_13(internal: Out[M250_13], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M250_13(_)=>
				val cont = internal !! M250_13(msg.msg)_
        if (count < max) {
          sendInternalChoice2(cont, external, count+1)
        } else {
          Continue(() => sendInternalChoice2(cont, external, 0))
        }
      case _ =>
        Stop()
    }
  }
  def sendInternalChoice2(internal: In[InternalChoice2], external: ConnectionManager, count: Int): Control = {
    internal ? {
      case msg @ MailFrom(_) =>
        external.send(msg)
        if (count < max) {
          receiveM250_9(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM250_9(msg.cont, external, 0))
        }
      case msg @ Quit_12() =>
        external.send(msg)
        if (count < max){
          receiveM221_11(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM221_11(msg.cont, external, 0))
        }
    }
  }
  def receiveM250_9(internal: Out[M250_9], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M250_9(_)=>
				val cont = internal !! M250_9(msg.msg)_
        if (count < max){
          sendInternalChoice1(cont, external, count+1)
        } else {
          Continue(() => sendInternalChoice1(cont, external, 0))
        }
      case _ =>
        Stop()
    }
  }
  def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int): Control = {
    internal ? {
      case msg @ RcptTo(_) =>
        external.send(msg)
        if(count < max){
          receiveM250_1(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM250_1(msg.cont, external, 0))
        }
      case msg @ Data() =>
        external.send(msg)
        if(count < max) {
          receiveM354_5(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM354_5(msg.cont, external, 0))
        }
      case msg @ Quit_8() =>
        external.send(msg)
        if(count < max) {
          receiveM221_7(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM221_7(msg.cont, external, 0))
        }
    }
  }
  def receiveM250_1(internal: Out[M250_1], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M250_1(_)=>
				val cont = internal !! M250_1(msg.msg)_
        if(count < max) {
          sendInternalChoice1(cont, external, count+1)
        } else {
          Continue(() => sendInternalChoice1(cont, external, 0))
        }
      case _ =>
        Stop()
    }
  }
  def receiveM354_5(internal: Out[M354], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M354(_)=>
				val cont = internal !! M354(msg.msg)_
        if(count < max){
          sendContent_4(cont, external, count+1)
        } else {
          Continue(() => sendContent_4(cont, external, 0))
        }
      case _ =>
        Stop()
    }
  }
  def sendContent_4(internal: In[Content], external: ConnectionManager, count: Int): Control = {
    internal ? {
      case msg @ Content(_) =>
        external.send(msg)
        if(count < max){
          receiveM250_3(msg.cont, external, count+1)
        } else {
          Continue(() => receiveM250_3(msg.cont, external, 0))
        }
    }
  }
  def receiveM250_3(internal: Out[M250_3], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M250_3(_)=>
				val cont = internal !! M250_3(msg.msg)_
        if(count < max){
          sendInternalChoice2(cont, external, count+1)
        } else {
          Continue(() => sendInternalChoice2(cont, external, 0))
        }
      case _ =>
        Stop()
    }
  }
  def receiveM221_7(internal: Out[M221_7], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M221_7(_)=>
        internal ! msg
        Stop()
      case _ =>
        Stop()
    }
  }
  def receiveM221_11(internal: Out[M221_11], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M221_11(_)=>
        internal ! msg
        Stop()
      case _ =>
        Stop()
    }
  }
  def receiveM221_15(internal: Out[M221_15], external: ConnectionManager, count: Int): Control = {
    external.receive() match {
      case msg @ M221_15(_)=>
        internal ! msg
        Stop()
      case _ =>
        Stop()
    }
  }
}