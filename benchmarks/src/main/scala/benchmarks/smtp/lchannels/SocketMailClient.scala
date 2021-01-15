package benchmarks.smtp.lchannels

import benchmarks.util.{logger, timer}
import lchannels.{In, SocketIn, SocketManager, SocketOut}

import java.util.Calendar
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object mailclient {
  def apply(Server: In[M220], pathname: String, iterations: Int, run: Int)(implicit ec: ExecutionContext, timeout: Duration): Unit = {
    val l = new logger(f"${pathname}/${iterations}_exec_time_run${run}.csv")
    println("[MC] Mail Client started")
    Server ? {
      case m220 @ M220(_) =>
        println(f"[MC] ↓ 220 ${m220.msg}")
        val resp = m220.cont !! Helo(m220.msg)_
        println(f"[MC] ↑ Helo ${m220.msg}")
        resp ? {
          case m250_1@M250_1(_) =>
            println(f"[MC] ↓ 250 ${m250_1.msg}")
            var m250cont = m250_1.cont

            println(f"[MC] ⟳ Running ${iterations} iterations.")
            for( iter <- 1 to iterations) {
              timer.start()
              val resp = m250cont !! MailFrom("chris@test.com")_
              //              println(f"[MC] ↑ MAIL FROM: chris@test.com")
              resp ? {
                case m250_2@M250_2(_) =>
                  //                  println(f"[MC] ↓ 250 ${m250_2.msg}")
                  val resp = m250_2.cont !! RcptTo("alceste@test.com")_
                  //                  println(f"[MC] ↑ RCPT TO: alceste@test.com")
                  resp ? {
                    case m250_3@M250_3(_) =>
                      //                      println(f"[MC] ↓ 250 ${m250_3.msg}")
                      val resp = m250_3.cont !! Data()_
                      //                      println(f"[MC] ↑ DATA")
                      resp ? {
                        case m354@M354(_) =>
                          //                          println(f"[MC] ↓ 354 ${m354.msg}")
                          val resp = m354.cont !! Content(f"ping ${iter}")_
                          //                          println(f"[MC] ↑ ping")
                          resp ? {
                            case m250_4@M250_4(_) =>
                              //                              println(f"[MC] ↓ 250 ${m250_4.msg}")
                              m250cont = m250_4.cont
                          }
                      }
                  }
              }
              l.log(f"${Calendar.getInstance.getTime.toString.split(" ")(3)} ${iter} ${timer.end().toString}")
            }
            val resp = m250cont !! Quit_2()_
            println(f"[MC] ↑ Quit")
            resp ? {
              case m221_2@M221_2(_) =>
                println(f"[MC] ↓ 221 ${m221_2.msg}")
            }
        }
    }
  }
}


object SocketClient extends App {
  // Helper method to ease external invocation
  def run() = main(Array())

  import java.io.{
    BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter
  }
  import java.net.Socket

  implicit val timeout = Duration.Inf

  class HelloSocketManager(socket: Socket)
    extends SocketManager(socket) {
    private val outB = new BufferedWriter(new OutputStreamWriter(out))

    override def streamer(x: Any) = x match {
      case Helo(hostname) => outB.write(f"HELO ${hostname}\r\n"); outB.flush();
      case MailFrom(addr) => outB.write(f"MAIL FROM: ${addr}\r\n"); outB.flush();
      case RcptTo(addr) => outB.write(f"RCPT TO: ${addr}\r\n"); outB.flush();
      case Data() => outB.write(f"DATA\r\n"); outB.flush();
      case Content(txt) => outB.write(f"${txt}\r\n.\r\n"); outB.flush();
      case Quit_1() | Quit_2() | Quit_3() => outB.write(f"QUIT\r\n"); outB.flush();
      case _ => close(); throw new Exception("[CM] Error: Unexpected message by Mon");
    }

    private val inB = new BufferedReader(new InputStreamReader(in))
    private val M220R = """220 ([\S]+) .*""".r
    private val M250R = """250 (.*)""".r
    private val M354R = """354 (.*)""".r
    private val M221R = """221 (.*)""".r

    var m250counter = 1

    override def destreamer() = inB.readLine() match {
      case M220R(msg) => M220(msg)(SocketOut[InternalChoice3](this));
      case M250R(msg) =>
        if(m250counter == 1){
          m250counter += 1
          M250_1(msg)(SocketOut[InternalChoice2](this))
        } else if (m250counter == 2) {
          m250counter += 1
          M250_2(msg)(SocketOut[InternalChoice1](this))
        } else if (m250counter == 3) {
          m250counter += 1
          M250_3(msg)(SocketOut[InternalChoice1](this))
        } else if (m250counter == 4) {
          m250counter = 2
          M250_4(msg)(SocketOut[InternalChoice2](this))
        }

      case M354R(msg) => M354(msg)(SocketOut[Content](this));
      case M221R(msg) => close(); M221_2(msg);
      case e => e
    }
  }

  println("[*] Connecting to 127.0.0.1:1025...")
  val conn = new Socket("127.0.0.1", 1025)
  val sktm = new HelloSocketManager(conn)
  val c = SocketIn[M220](sktm)
  mailclient(c, args(0), args(1).toInt, args(2).toInt)(global, timeout)
}