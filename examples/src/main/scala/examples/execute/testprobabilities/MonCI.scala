package examples.execute.testprobabilities

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
			var counter=0
			var alert=false
			val prob=0.7
		}
		object Correct_1 {
			var counter=0
			var alert=false
			val prob=0.2
		}
		object Incorrect_2 {
			var counter=0
			var alert=false
			val prob=0.8
		}
		object New_5 {
			var counter=0
			var alert=false
			val prob=0.29
		}
		object Quit_4 {
			var counter=0
			var alert=false
			val prob=0.01
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
	def checkExternalChoice1Intervals(): Unit = {
		val (pmin_Guess, pmax_Guess) = calculateInterval(labels.Guess_3.counter, labels.ExternalChoice1.counter)
		if(pmin_Guess >= labels.Guess_3.prob || pmax_Guess <= labels.Guess_3.prob) {
			if(!labels.Guess_3.alert){
				println(f"[MON] **ALERT** Guess [${labels.Guess_3.prob}] outside interval: $pmin_Guess,$pmax_Guess")
				labels.Guess_3.alert = true
			}
		} else {
			if(labels.Guess_3.alert){
				println(f"[MON] **INFO** Guess [${labels.Guess_3.prob}] within interval: $pmin_Guess,$pmax_Guess")
				labels.Guess_3.alert = false
			}
		}
		val (pmin_New, pmax_New) = calculateInterval(labels.New_5.counter, labels.ExternalChoice1.counter)
		if(pmin_New >= labels.New_5.prob || pmax_New <= labels.New_5.prob){
			if(!labels.New_5.alert) {
				println(f"[MON] **ALERT** New [${labels.New_5.prob}] outside interval: $pmin_New,$pmax_New")
				labels.New_5.alert = true
			}
		} else {
			if (labels.New_5.alert) {
				println(f"[MON] **INFO** New [${labels.New_5.prob}] within interval: $pmax_New,$pmax_New")
				labels.New_5.alert = false
			}
		}
		val (pmin_Quit, pmax_Quit) = calculateInterval(labels.Quit_4.counter, labels.ExternalChoice1.counter)
		if(pmin_Quit >= labels.Quit_4.prob || pmax_Quit <= labels.Quit_4.prob){
			if(!labels.Quit_4.alert) {
				println(f"[MON] **ALERT** Quit [${labels.Quit_4.prob}] outside interval: $pmin_Quit,$pmax_Quit")
				labels.Quit_4.alert = true
			}
		} else {
			if (labels.Quit_4.alert) {
				println(f"[MON] **INFO** Quit [${labels.Quit_4.prob}] within interval: $pmin_Quit,$pmax_Quit")
				labels.Quit_4.alert = false
			}
		}
	}
	def checkInternalChoice1Intervals(): Unit = {
		val (pmin_Incorrect, pmax_Incorrect) = calculateInterval(labels.Incorrect_2.counter, labels.InternalChoice1.counter)
		if (pmin_Incorrect >= labels.Incorrect_2.prob || pmax_Incorrect <= labels.Incorrect_2.prob) {
			if (!labels.Incorrect_2.alert) {
				println(f"[MON] **ALERT** Incorrect [${labels.Incorrect_2.prob}] outside interval: $pmin_Incorrect,$pmax_Incorrect")
				labels.Incorrect_2.alert = true
			}
		} else {
			if (labels.Incorrect_2.alert) {
				println(f"[MON] **INFO** Incorrect [${labels.Incorrect_2.prob}] within interval: $pmin_Incorrect,$pmax_Incorrect")
				labels.Incorrect_2.alert = false
			}
		}
		val (pmin_Correct, pmax_Correct) = calculateInterval(labels.Correct_1.counter, labels.InternalChoice1.counter)
		if (pmin_Correct >= labels.Correct_1.prob || pmax_Incorrect <= labels.Correct_1.prob) {
			if (!labels.Correct_1.alert) {
				println(f"[MON] **ALERT** Correct [${labels.Correct_1.prob}] outside interval: $pmin_Correct,$pmax_Correct")
				labels.Correct_1.alert = true
			}
		} else {
			if (labels.Correct_1.alert) {
				println(f"[MON] **INFO** Correct [${labels.Correct_1.prob}] within interval: $pmin_Correct,$pmax_Correct")
				labels.Correct_1.alert = false
			}
		}
	}
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		labels.ExternalChoice1.counter+=1
		external.receive() match {
			case msg @ Guess(_)=>
				labels.Guess_3.counter+=1
				checkExternalChoice1Intervals()
				val cont = internal !! Guess(msg.num)_
				if (count < max) {
					sendInternalChoice1(cont, external, count+1)
				} else { tailcall(sendInternalChoice1(cont, external,0)) }
			case msg @ New() =>
				labels.New_5.counter+=1
				checkExternalChoice1Intervals()
				val cont = internal !! New() _
				if (count < max) {
					receiveExternalChoice1(cont, external, count + 1)
				} else { tailcall(receiveExternalChoice1(cont, external, 0)) }
			case msg @ Quit()=>
				labels.Quit_4.counter+=1
				checkExternalChoice1Intervals()
					internal ! msg;
					done()
			case _ => done()
		}
	}
	def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		labels.InternalChoice1.counter+=1
		internal ? {
			case msg @ Correct(_) =>
				labels.Correct_1.counter+=1
				checkInternalChoice1Intervals()
				external.send(msg)
				if (count < max) {
					receiveExternalChoice1(msg.cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(msg.cont, external, 0)) }
			case msg @ Incorrect() =>
				labels.Incorrect_2.counter+=1
				checkInternalChoice1Intervals()
				external.send(msg)
				if (count < max) {
					receiveExternalChoice1(msg.cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(msg.cont, external, 0)) }
		}
	}
	def calculateInterval(count: Double, trials: Int): (Double, Double) = {
		val prob = count/trials
		val err = zvalue/(2*math.sqrt(trials))
		(prob-err, prob+err)
	}
}