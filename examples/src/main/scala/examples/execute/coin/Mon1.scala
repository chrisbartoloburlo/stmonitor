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
		}
		object Tails_2 {
			var counter = 0
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
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int): TailRec[Unit] = {
		labels.ExternalChoice1.counter+=1
		external.receive() match {
			case msg @ Heads()=>
				val cont = internal !! Heads()_

				labels.Heads_1.counter+=1

				val (pmint, pmaxt) = calculateInterval(labels.Tails_2.counter, labels.ExternalChoice1.counter)
				println(f"[MON] ?TAILS()[0.5] | interval: $pmint,$pmaxt | received ${labels.Tails_2.counter} Heads out of ${labels.ExternalChoice1.counter}")

				val (pmin, pmax) = calculateInterval(labels.Heads_1.counter, labels.ExternalChoice1.counter)
//				if(pmin >= 0.5 || pmax <= 0.5) {
					println(f"[MON] ?HEADS()[0.5] | interval: $pmin,$pmax | received ${labels.Heads_1.counter} Heads out of ${labels.ExternalChoice1.counter}")

				if (count < max) {
					receiveExternalChoice1(cont, external, count+1)
				} else { tailcall(receiveExternalChoice1(cont, external,0)) }
			case msg @ Tails()=>
				val cont = internal !! Tails() _

				labels.Tails_2.counter+=1

				val (pminh, pmaxh) = calculateInterval(labels.Heads_1.counter, labels.ExternalChoice1.counter)
				println(f"[MON] ?HEADS()[0.5] | interval: $pminh,$pmaxh | received ${labels.Heads_1.counter} Heads out of ${labels.ExternalChoice1.counter}")

				val (pmin, pmax) = calculateInterval(labels.Tails_2.counter, labels.ExternalChoice1.counter)
//				if(pmin >= 0.5 || pmax <= 0.5) {
					println(f"[MON] ?TAILS()[0.5] | interval $pmin,$pmax | received ${labels.Tails_2.counter} Heads out of ${labels.ExternalChoice1.counter}")

				if (count < max) {
					receiveExternalChoice1(cont, external, count + 1)
				} else {
				tailcall(receiveExternalChoice1(cont, external, 0)) }
			case _ => done()
		}
	}
	def calculateInterval(count: Double, trials: Int): (Double, Double) = {
		val prob = count/trials
		val err = zvalue/(2*math.sqrt(trials))
		(prob-err, prob+err)
	}
}