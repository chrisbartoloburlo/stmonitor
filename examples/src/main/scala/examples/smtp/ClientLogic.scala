package examples.smtp

import java.util.Calendar
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import lchannels.In

import examples.util.{logger, timer}

object ClientLogic {
  def apply(server: In[M220], pathname: String, iterations: Int, run: Int)(implicit ec: ExecutionContext, timeout: Duration) {
    val l = new logger(f"${pathname}/${iterations}_exec_time_run${run}.csv")
    println("[MC] Mail Client started")
    server ? {
      case m220 @ M220(_) =>
        println(f"[MC] ↓ 220 ${m220.msg}")
        val resp = m220.cont !! Helo(m220.msg)_
        println(f"[MC] ↑ Helo ${m220.msg}")
        resp ? {
          case m250_1@M250_13(_) =>
            println(f"[MC] ↓ 250 ${m250_1.msg}")
            var m250cont = m250_1.cont

            println(f"[MC] ⟳ Running ${iterations} iterations.")
            for( iter <- 1 to iterations) {
              timer.start()
              val resp = m250cont !! MailFrom("chris@test.com")_ //↑ MAIL FROM: chris@test.com
//              println("↑ MAIL FROM: chris@test.com")
              resp ? {
                case m250_2@M250_9(_) => //↓ 250 ${m250_2.msg}
//                  println(f"↓ 250 ${m250_2.msg}")
                  val resp = m250_2.cont !! RcptTo("discard@localhost")_ //↑ RCPT TO: discard@localhost
//                  println("↑ RCPT TO: discard@localhost")
                  resp ? {
                    case m250_3@M250_1(_) => //↓ 250 ${m250_3.msg}
//                      println(f"↓ 250 ${m250_3.msg}")
                      val resp = m250_3.cont !! Data()_ //↑ DATA
//                      println("↑ DATA")
                      resp ? {
                        case m354@M354(_) => //↓ 354 msg
//                          println(f"↓ 354 ${m354.msg}")
                          val resp = m354.cont !! Content(f"ping ${iter}")_ //↑ ping
//                          println(f"↑ ping $iter")
                          resp ? {
                            case m250_4@M250_3(_) => //↓ 250 msg
//                              println(f"↓ 250 ${m250_4.msg}")
                              m250cont = m250_4.cont
                          }
                      }
                  }
              }
              l.log(f"${Calendar.getInstance.getTime.toString.split(" ")(3)} ${iter} ${timer.end().toString}")
            }
            val resp = m250cont !! Quit_12() _
            println(f"[MC] ↑ Quit")
            resp ? {
              case m221_2@M221_11(_) =>
                println(f"[MC] ↓ 221 ${m221_2.msg}")
            }
        }
      case e => println(f"$e")
    }
  }
}
