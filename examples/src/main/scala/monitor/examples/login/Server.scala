package monitor.examples.login

import lchannels.In

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class Server(Client: In[Login])(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
  override def run(): Unit = {
    println("[S] Server started, to terminate press CTRL+c")
    Client ? {
      case l @ Login(_, _, _) =>
        println(f"[S] Received Login(${l.uname}, ${l.pwd}, ${l.token})")
        println(f"[S] Sending Retry()")
        val resp = l.cont !! Retry()_
        resp ? {
          case l @ Login(_, _, _) =>
            println(f"[S] Received Login(${l.uname}, ${l.pwd}, ${l.token})")
            val id = "id123"
            println(f"[S] Sending Success(${id})")
            l.cont ! Success(id)
        }
    }
    println("[S] Server terminated")
  }
}