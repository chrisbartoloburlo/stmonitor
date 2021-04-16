package examples.auth

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.util.Random

import lchannels.In

object Server {
  def apply(Client: In[Auth])(implicit ec: ExecutionContext, timeout: Duration) {
    var authChn = Client
    println("[S] Server started, to terminate press CTRL+c")
    var notAuth = true
    while(notAuth){
      authChn ? {
        case msg @ Auth(_, _) =>
          println(f"[S] Received Auth(${msg.uname}, ${msg.pwd})")
          val token = Random.alphanumeric.filter(_.isLetter).take(10).mkString
          println(f"[S] Sending Succ($token)")
          var reqChn = msg.cont !! Succ(token)_
          notAuth = false
          while(!notAuth){
            reqChn ? {
              case msg @ Get(_, _) =>
                println(f"[S] Received Get(${msg.resource}, ${msg.reqTok})")
                if(token!=msg.reqTok){
                  println("[S] Tokens do not match: sending Timeout()")
                  authChn = msg.cont !! Timeout()
                  notAuth = true
                } else {
                  println(f"[S] Sending Res(content)")
                  reqChn = msg.cont !! Res("content")
                }
              case msg @ Rvk(_) =>
                println(f"[S] Received Rvk(${msg.rvkTok}): terminating")
                return
            }
          }
      }
    }
  }
}
