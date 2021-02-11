package examples.execute.coin

import lchannels.Out
import monitor.util.ConnectionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.control.TailCalls.{TailRec, done, tailcall}

class Mon(external: ConnectionManager, internal: Out[ExternalChoice1], max: Int, zvalue: Double)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object labels {
		object ExternalChoice1 {
			var counter = 0
		}
		object Heads_1 {
			var counter = 0
			val prob = 0.5
			var warn = false
		}
		object Tails_2 {
			var counter = 0
			val prob = 0.5
			var warn = false
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
			case msg @ Heads()=>
				labels.Heads_1.counter+=1
				checkExternalChoice1Intervals()
				val cont = internal !! Heads()_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ Tails()=>
				labels.Tails_2.counter+=1
				checkExternalChoice1Intervals()
				val cont = internal !! Tails()_
				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case _ => done()
		}
	}
	def checkExternalChoice1Intervals(): Unit = {
		val (pmin_Heads,pmax_Heads) = calculateInterval(labels.Heads_1.counter, labels.ExternalChoice1.counter)
		if(pmin_Heads >= labels.Heads_1.prob || pmax_Heads <= labels.Heads_1.prob) {
			if(!labels.Heads_1.warn){
				println(f"[MON] **WARN** ?Heads[${labels.Heads_1.prob}] outside interval [$pmin_Heads,$pmax_Heads]")
				labels.Heads_1.warn = true
			}
		} else {
			if(labels.Heads_1.warn){
				println(f"[MON] **INFO** ?Heads[${labels.Heads_1.prob}] within interval [$pmin_Heads,$pmax_Heads]")
				labels.Heads_1.warn = false
			}
		}
		val (pmin_Tails,pmax_Tails) = calculateInterval(labels.Tails_2.counter, labels.ExternalChoice1.counter)
		if(pmin_Tails >= labels.Tails_2.prob || pmax_Tails <= labels.Tails_2.prob) {
			if(!labels.Tails_2.warn){
				println(f"[MON] **WARN** ?Tails[${labels.Tails_2.prob}] outside interval [$pmin_Tails,$pmax_Tails]")
				labels.Tails_2.warn = true
			}
		} else {
			if(labels.Tails_2.warn){
				println(f"[MON] **INFO** ?Tails[${labels.Tails_2.prob}] within interval [$pmin_Tails,$pmax_Tails]")
				labels.Tails_2.warn = false
			}
		}
	}
	def calculateInterval(count: Double, trials: Int): (Double, Double) = {
		val prob = count/trials
		val err = zvalue/(2*math.sqrt(trials))
		(prob-err,prob+err)
	}
}