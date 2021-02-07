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
class MonMC(external: ConnectionManager, internal: Out[ExternalChoice1], max: Int)(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {
	object payloads {
		object Guess_3 {
			var num: Int = _
			val id=2
		}
		object Correct_1 {
			var ans: Int = _
			val id=0
		}
		object Incorrect_2 {
			val id=1
		}
		object New_5 {
			val id=4
		}
		object Quit_4 {
			val id=3
		}
	}
	private val transitionMatrix = Array.ofDim[Double](6,6)

	override def run(): Unit = {
    println("[Mon] Monitor started")
    println("[Mon] Setting up connection manager")
		external.setup()
		receiveExternalChoice1(internal, external, 0, 5).result
		for (row <- 0 until 6) {
			var sum = transitionMatrix(row).take(5).sum
			if(sum==0) sum=1
			for(col <- 0 until 6){
				transitionMatrix(row)(col) = transitionMatrix(row)(col)/sum
			}
			if(transitionMatrix(row).sum==0) transitionMatrix(row)(5)=1
//			if(transitionMatrix(row)(row)==0) transitionMatrix(row)(row)=math.abs(transitionMatrix(row).take(5).sum-1)
		}
		for {
			row <- 0 until 6
			col <- 0 until 6
		} if(col==5) print(transitionMatrix(row)(col)+"\n") else print(transitionMatrix(row)(col)+" ")
		external.close()
  }
	def receiveExternalChoice1(internal: Out[ExternalChoice1], external: ConnectionManager, count: Int, prev: Int): TailRec[Unit] = {
		external.receive() match {
			case msg @ Guess(_)=>
				if(msg.num >= 0 && msg.num <= 50){
					transitionMatrix(prev)(payloads.Guess_3.id)+=1
					val cont = internal !! Guess(msg.num)_
					payloads.Guess_3.num = msg.num
					if (count < max) {
						sendInternalChoice1(cont, external,count+1, payloads.Guess_3.id)
					} else { tailcall(sendInternalChoice1(cont, external,0, payloads.Guess_3.id)) }
				} else {
				done() }
			case msg @ New() =>
				transitionMatrix(prev)(payloads.New_5.id)+=1
				val cont = internal !! New()_
				if (count < max) {
					receiveExternalChoice1(cont, external,count+1, payloads.New_5.id)
				} else { tailcall(receiveExternalChoice1(cont, external,0, payloads.New_5.id)) }
			case msg @ Quit()=>
				transitionMatrix(prev)(payloads.Quit_4.id)+=1
				internal ! msg; done()
			case _ => done()
		}
	}
	def sendInternalChoice1(internal: In[InternalChoice1], external: ConnectionManager, count: Int, prev: Int): TailRec[Unit] = {
		internal ? {
			case msg @ Correct(_) =>
				transitionMatrix(prev)(payloads.Correct_1.id)+=1
				if(msg.ans==payloads.Guess_3.num){
					external.send(msg)
					if (count < max) {
						receiveExternalChoice1(msg.cont, external, count+1, payloads.Correct_1.id)
					} else { tailcall(receiveExternalChoice1(msg.cont, external, 0, payloads.Correct_1.id)) }
				} else {
				done() }
			case msg @ Incorrect() =>
				transitionMatrix(prev)(payloads.Incorrect_2.id)+=1
				external.send(msg)
				if (count < max) {
					receiveExternalChoice1(msg.cont, external, count+1, payloads.Incorrect_2.id)
				} else { tailcall(receiveExternalChoice1(msg.cont, external, 0, payloads.Incorrect_2.id)) }
		}
	}
}