package benchmarks.smtp.dummymonitored

import benchmarks.util.{logger, timer}

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.net.{InetAddress, Socket}
import java.util.Calendar

class mailclient(pathname: String, iterations: Int, run: Int) extends Runnable {
  override def run(): Unit = {

    val M220R = """220 ([\S]+) .*""".r
    val M250R = """250 (.*)""".r
    val M354R = """354 (.*)""".r
    val M221R = """221 (.*)""".r

    val s = new Socket(InetAddress.getByName("localhost"), 1025)
    val outB: BufferedWriter = new BufferedWriter(new OutputStreamWriter(s.getOutputStream))
    val inB: BufferedReader = new BufferedReader(new InputStreamReader(s.getInputStream))

    val l = new logger(f"${pathname}/${iterations}_exec_time_run${run}.csv")

    println("[MC] Mail Client started")
    inB.readLine() match {
      case M220R(hostname) =>
        println(f"[MC] ↓ 220 ${hostname}")
        outB.write(f"HELO ${hostname}\r\n"); outB.flush()
        println(f"[MC] ↑ Helo ${hostname}")
        inB.readLine() match {
          case msg @ M250R(_) =>
            println(f"[MC] ↓ ${msg}")

            println(f"[MC] ⟳ Running ${iterations} iterations.")
            for(iter <- 1 to iterations) {
              timer.start()
              outB.write(f"MAIL FROM: chris@test.com\r\n"); outB.flush()
//              println(f"[MC] ↑ MAIL FROM: chris@test.com")
              inB.readLine() match {
                case msg @ M250R(_) =>
//                  println(f"[MC] ↓ 250 ${m250_2.msg}")
                  outB.write(f"RCPT TO: alceste@test.com\r\n"); outB.flush()
//                  println(f"[MC] ↑ RCPT TO: alceste@test.com")
                  inB.readLine() match {
                    case msg @ M250R(_) =>
//                      println(f"[MC] ↓ 250 ${m250_3.msg}")
                      outB.write(f"DATA\r\n"); outB.flush();
//                      println(f"[MC] ↑ DATA")
                      inB.readLine() match {
                        case msg @ M354R(_) =>
//                          println(f"[MC] ↓ 354 ${m354.msg}")
                          outB.write(f"ping ${iter}\n"); outB.flush()
//                          println(f"[MC] ↑ ping")
                          inB.readLine() match {
                            case msg @ M250R(_) =>
//                              println(f"[MC] ↓ 250 ${m250_4.msg}")
                          }
                      }
                  }
              }
              l.log(f"${Calendar.getInstance.getTime.toString.split(" ")(3)} ${iter} ${timer.end().toString}")
            }
            outB.write(f"QUIT\r\n"); outB.flush()
            println(f"[MC] ↑ Quit")
            inB.readLine() match {
              case msg @ M221R(_) =>
                println(f"[MC] ↓ ${msg}")
            }
            inB.close()
            outB.close()
        }
      case e =>
        println(e)
    }
  }
}

