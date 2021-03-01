package examples.auth

import lchannels.{In, LocalChannel}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

//object Server {
//  def apply(Client: In[Auth])(implicit ec: ExecutionContext, timeout: Duration) {
//    println("[S] Server started, to terminate press CTRL+c")
//    Client ? {
//      case l @ Auth(_, _) =>
//        println(f"[S] Received Auth(${l.uname}, ${l.pwd})")
//        val count = 1
//        println(f"[S] Sending Fail(${count})")
//        val resp = l.cont !! Fail(count)_
//        resp ? {
//          case l @ Auth(_, _) =>
//            println(f"[S] Received Auth(${l.uname}, ${l.pwd})")
//            val tok = "tok123"
//            println(f"[S] Sending Succ(${tok})")
//            l.cont ! Succ(tok)
//        }
//    }
//    println("[S] Server terminated")
//  }
//}

//object MonitoredServer extends App {
//  def run() = main(Array())
//  val timeout = Duration.Inf
//
//  val (in, out) = LocalChannel.factory[Auth]()
//  val mon = new Mon(new ConnectionManager(1330), out, 300)(global, timeout)
//
//  val monThread = new Thread {
//    override def run(): Unit = {
//      mon.run()
//    }
//  }
//
//  monThread.start()
//  Server(in)(global, timeout)
//}
