package examples.execute.probabilities

import lchannels.{In, Out}
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}

//S_game=rec X.(
// &{?Guess(num: Int)[0.7].+{ !Correct(ans: Int)[0.2].X, !Incorrect()[0.8].X },
// ?New()[0.2].X,
// ?Quit()[0.1]} )
class MonCI(external: ConnectionManager, internal: Out[ExternalChoice1], max: Int, zvalue: Double)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object labels {
		object Guess_3 {
			val id=2
			var counter=0
		}
		object Correct_1 {
			val id=0
			var counter=0
		}
		object Incorrect_2 {
			val id=1
			var counter=0
		}
		object New_5 {
			val id=4
			var counter=0
		}
		object Quit_4 {
			val id=3
			var counter=0
		}
		object ExternalChoice1 {
			var counter=0
		}
		object InternalChoice1 {
			var counter=0
		}
	}

	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		receiveExternalChoice1(internal, external, 0).result
		external.close()
  }
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		labels.ExternalChoice1.counter+=1
		external.receive() match {
			case msg @ Guess(_)=>
				labels.Guess_3.counter+=1
				val (pmin, pmax) = calculateInterval(labels.Guess_3.counter, labels.ExternalChoice1.counter)
				if(pmin <= 0.7 && pmax >= 0.7){
					val cont = internal !! Guess(msg.num)_
					if (count < max) {
						sendInternalChoice1(cont, external, count+1)
					} else { tailcall(sendInternalChoice1(cont, external,0)) }
				} else {
				  println(f"[MON] Receive Guess(num: Int)[0.7] not in $pmin,$pmax")
					done()
				}
			case msg @ New() =>
				labels.New_5.counter+=1
				val (pmin, pmax) = calculateInterval(labels.New_5.counter, labels.ExternalChoice1.counter)
				if(pmin <= 0.29 && pmax >= 0.29) {
					val cont = internal !! New() _
					if (count < max) {
						receiveExternalChoice1(cont, external, count + 1)
					} else {
						tailcall(receiveExternalChoice1(cont, external, 0))
					}
				} else {
					println(f"[MON] Receive New()[0.29] not in $pmin,$pmax")
					done()
				}
			case msg @ Quit()=>
				labels.Quit_4.counter+=1
				val (pmin, pmax) = calculateInterval(labels.Quit_4.counter, labels.ExternalChoice1.counter)
				if(pmin <= 0.01 && pmax >= 0.01) {
					internal ! msg;
					done()
				} else {
					println(f"[MON] Receive Quit()[0.01] not in $pmin,$pmax")
					done() }
			case _ => done()
		}
	}
	def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		labels.InternalChoice1.counter+=1
		internal ? {
			case msg @ Correct(_) =>
				labels.Correct_1.counter+=1
				val (pmin, pmax) = calculateInterval(labels.Correct_1.counter, labels.InternalChoice1.counter)
				if(pmin <= 0.2 && pmax >= 0.2){
					external.send(msg)
					if (count < max) {
						receiveExternalChoice1(msg.cont, external, count+1)
					} else { tailcall(receiveExternalChoice1(msg.cont, external, 0)) }
				} else {
					println(f"[MON] Send Correct(ans: Int)[0.2] not in $pmin,$pmax")
					done()
				}
			case msg @ Incorrect() =>
				labels.Incorrect_2.counter+=1
				val (pmin, pmax) = calculateInterval(labels.Incorrect_2.counter, labels.InternalChoice1.counter)
//				if(pmin <= 0.8 && pmax >= 0.8){
					external.send(msg)
					if (count < max) {
						receiveExternalChoice1(msg.cont, external, count+1)
					} else { tailcall(receiveExternalChoice1(msg.cont, external, 0)) }
//				} else {
//					println(f"[MON] Send Incorrect()[0.8] not in $pmin,$pmax")
//					done()
//				}
		}
	}
	def calculateInterval(count: Double, trials: Int): (Double, Double) = {
		val prob = count/trials
		val err = zvalue/(2*math.sqrt(trials))
		(prob-err, prob+err)
	}
}