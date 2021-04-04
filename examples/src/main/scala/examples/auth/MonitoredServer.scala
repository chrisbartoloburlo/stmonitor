package examples.auth

import lchannels.{In, LocalChannel}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.util.Random

//object Server {
//  def apply(Client: In[Auth])(implicit ec: ExecutionContext, timeout: Duration) {
//    var authChn = Client
//    println("[S] Server started, to terminate press CTRL+c")
//    var notAuth = true
//    while(notAuth){
//      authChn ? {
//        case msg @ Auth(_, _) =>
//          println(f"[S] Received Auth(${msg.uname}, ${msg.pwd})")
//          val token = Random.alphanumeric.filter(_.isLetter).take(10).mkString
//          println(f"[S] Sending Succ($token)")
//          var reqChn = msg.cont !! Succ(token)_
//          notAuth = false
//          while(!notAuth){
//            reqChn ? {
//              case msg @ Get(_, _) =>
//                println(f"[S] Received Get(${msg.resource}, ${msg.reqTok})")
//                if(token!=msg.reqTok){
//                  println("[S] Tokens do not match: sending Timeout()")
//                  authChn = msg.cont !! Timeout()
//                  notAuth = true
//                } else {
//                  println(f"[S] Sending Res(content)")
//                  reqChn = msg.cont !! Res("content")
//                }
//              case msg @ Rvk(_) =>
//                println(f"[S] Received Rvk(${msg.rvkTok}): terminating")
//                sys.exit(0)
//            }
//          }
//      }
//    }
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
