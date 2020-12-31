package benchmarks.smtp.monitored

import akka.actor._
import lchannels.{In, Out}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class Mon(Internal: Out[M220])(implicit ec: ExecutionContext, timeout: Duration) extends Actor {
  object payloads {
		object M220 {
			var msg: String = _
		}
		object Helo {
			var hostname: String = _
		}
		object M250_1 {
			var msg: String = _
		}
		object MailFrom {
			var addr: String = _
		}
		object M250_2 {
			var msg: String = _
		}
		object RcptTo {
			var addr: String = _
		}
		object M250_3 {
			var msg: String = _
		}
		object Data {
		}
		object M354 {
			var msg: String = _
		}
		object Content {
			var txt: String = _
		}
		object M250_4 {
			var msg: String = _
		}
		object Quit_1 {
		}
		object M221_1 {
			var msg: String = _
		}
		object Quit_2 {
		}
		object M221_2 {
			var msg: String = _
		}
		object Quit_3 {
		}
		object M221_4 {
			var msg: String = _
		}
	}
  def receive: Receive = {
    case MonStart =>
      println("[Mon] Monitor started")
      println("[Mon] Setting up connection manager")
      val cm = new ConnectionManager()
      cm.setup()
      receiveM220(Internal, cm)
      cm.close()
  }
  def receiveM220(internal: Out[M220], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M220(_)=>
				val cont = internal !! M220(msg.msg)_
				sendInternalChoice3(cont, External)
      case _ =>
    }
  }
  def sendInternalChoice3(internal: In[InternalChoice3], External: ConnectionManager): Any = {
    internal ? {
      case msg @ Helo(_) =>
        External.send(msg)
				receiveM250_1(msg.cont, External)
      case msg @ Quit_3() =>
        External.send(msg)
        receiveM221_3(msg.cont, External)
    }
  }
  def receiveM250_1(internal: Out[M250_1], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M250(_)=>
				val cont = internal !! M250_1(msg.msg)_
				sendInternalChoice2(cont, External)
      case err =>
        println(f"[Mon] **VIOLATION** received: ${err}")
    }
  }
  def sendInternalChoice2(internal: In[InternalChoice2], External: ConnectionManager): Any = {
    internal ? {
      case msg @ MailFrom(_) =>
        External.send(msg)
				receiveM250_2(msg.cont, External)
      case msg @ Quit_2() =>
        External.send(msg)
				receiveM221_2(msg.cont, External)
    }
  }
  def receiveM250_2(internal: Out[M250_2], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M250(_)=>
				val cont = internal !! M250_2(msg.msg)_
				sendInternalChoice1(cont, External)
      case _ =>
    }
  }
  def sendInternalChoice1(internal: In[InternalChoice1], External: ConnectionManager): Any = {
    internal ? {
      case msg @ RcptTo(_) =>
        External.send(msg)
				receiveM250_3(msg.cont, External)
      case msg @ Data() =>
        External.send(msg)
				receiveM354(msg.cont, External)
      case msg @ Quit_1() =>
        External.send(msg)
				receiveM221_1(msg.cont, External)
    }
  }
  def receiveM250_3(internal: Out[M250_3], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M250(_)=>
				val cont = internal !! M250_3(msg.msg)_
				sendInternalChoice1(cont, External)
      case _ =>
    }
  }
  def receiveM354(internal: Out[M354], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M354(_)=>
				val cont = internal !! M354(msg.msg)_
				sendContent(cont, External)
      case _ =>
    }
  }
  def sendContent(internal: In[Content], External: ConnectionManager): Any = {
    internal ? {
      case msg @ Content(_) =>
        External.send(msg)
				receiveM250_4(msg.cont, External)
    }
  }
  def receiveM250_4(internal: Out[M250_4], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M250(_)=>  //FIXME change from M250_4(_) to M250
				val cont = internal !! M250_4(msg.msg)_
				sendInternalChoice2(cont, External)
      case _ =>
    }
  }
  def receiveM221_1(internal: Out[M221_1], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M221(_)=>  //FIXME
        internal ! M221_1(msg.msg)  //FIXME
      case _ =>
    }
  }
  def receiveM221_2(internal: Out[M221_2], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M221(_)=>  //FIXME
        internal ! M221_2(msg.msg) //FIXME
      case _ =>
    }
  }
  def receiveM221_3(internal: Out[M221_3], External: ConnectionManager): Any = {
    External.receive() match {
      case msg @ M221(_)=> //FIXME change from M221_3(_) to M221(_)
        internal ! M221_3(msg.msg)  //FIXME change from msg.msg to M221_3(msg.msg)
      case _ =>
    }
  }
}