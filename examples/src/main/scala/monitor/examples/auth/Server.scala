package monitor.examples.auth

import lchannels.In

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class Server(Client: In[Auth])(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
  override def run(): Unit = {
    println("[S] Server started, to terminate press CTRL+c")
    Client ? {
      case l @ Auth(_, _) =>
        println(f"[S] Received Auth(${l.uname}, ${l.pwd})")
        val count = 1
        println(f"[S] Sending Fail(${count})")
        val resp = l.cont !! Fail(count)_
        resp ? {
          case l @ Auth(_, _) =>
            println(f"[S] Received Login(${l.uname}, ${l.pwd})")
            val tok = "id123"
            println(f"[S] Sending Succ(${tok})")
            l.cont ! Succ(tok)
        }
    }
    println("[S] Server terminated")
  }
}