package examples.execute.coin

import lchannels.Out
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}

class Mon1(external: ConnectionManager, internal: Out[ExternalChoice1], max: Int, zvalue: Double)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object labels {
		object Heads_1 {
			var counter = 0
			var alert = false

		}
		object Tails_2 {
			var counter = 0
			var alert = false
		}
		object ExternalChoice1 {
			var counter = 0
		}
	}
	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		receiveExternalChoice1(internal, external, 0).result
    external.close()
  }
//	S_test=rec X.(&{?Heads[0.5].X, ?Tails[0.5].X})
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		labels.ExternalChoice1.counter+=1
		external.receive() match {
			case msg @ Heads()=>
				val cont = internal !! Heads()_
				labels.Heads_1.counter+=1
				checkExternalChoice1Intervals()
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ Tails()=>
				val cont = internal !! Tails()_
				labels.Tails_2.counter+=1
				checkExternalChoice1Intervals()
				if (count < max) {
					receiveExternalChoice1(cont, external, count + 1)
				} else {
				tailcall(receiveExternalChoice1(cont, external, 0)) }
			case _ => done()
		}
	}
	def checkExternalChoice1Intervals(): Unit = {
		val (pmin_Heads, pmax_Heads) = calculateInterval(labels.Heads_1.counter, labels.ExternalChoice1.counter)
		if(pmin_Heads >= 0.5 || pmax_Heads <= 0.5) {
			if(!labels.Heads_1.alert){
				println(f"[MON] **ALERT** ?HEADS()[0.5] outside interval: $pmin_Heads,$pmax_Heads")
				labels.Heads_1.alert = true
			}
		} else {
			if(labels.Heads_1.alert){
				println(f"[MON] **INFO** ?HEADS()[0.5] within interval: $pmin_Heads,$pmax_Heads ")
				labels.Heads_1.alert = false
			}
		}
		val (pmin_Tails, pmax_Tails) = calculateInterval(labels.Tails_2.counter, labels.ExternalChoice1.counter)
		if(pmin_Tails >= 0.5 || pmax_Tails <= 0.5){
			if(!labels.Tails_2.alert) {
				println(f"[MON] **ALERT** ?TAILS()[0.5] outside interval: $pmin_Tails,$pmax_Tails")
				labels.Tails_2.alert = true
			}
		} else {
			if (labels.Tails_2.alert) {
				println(f"[MON] **INFO** ?TAILS()[0.5] within interval: $pmin_Tails,$pmax_Tails")
				labels.Tails_2.alert = false
			}
		}
	}
	def calculateInterval(count: Double, trials: Int): (Double, Double) = {
		val prob = count/trials
		val err = zvalue/(2*math.sqrt(trials))
		(prob-err, prob+err)
	}
}