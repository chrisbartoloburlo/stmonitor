package benchmarks.pingpong.lchannels

import java.util.concurrent.LinkedBlockingQueue

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import benchmarks.pingpong.monitored.{Ping, Pong}
import lchannels.{In, SocketManager, StreamManager, StreamOut}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.{Duration, DurationInt}

object ponger {
  def apply(Pinger: In[Ping])(implicit ec: ExecutionContext, timeout: Duration) = {
    println("[Ponger] Ponger started, to terminate press CTRL+c")
    var resp = Pinger
    while (true) {
      resp ? {
        case ping @ Ping() =>
          resp = ping.cont !! Pong()
      }
    }
  }
}

object ConnectionManager extends App {
  // Helper method to ease external invocation
  def run() = main(Array())

  implicit val timeout = Duration.Inf

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  var requestsQueue = new LinkedBlockingQueue[HttpRequest]()
  var responsesQueue = new LinkedBlockingQueue[Promise[HttpResponse]]()

  val requestQueue: HttpRequest => Future[HttpResponse] = {
    case ping @ HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      val p = Promise[HttpResponse]()
      requestsQueue.put(ping)
      responsesQueue.put(p)
      //      println("CM queued ping")
      p.future
  }

  val bindingFuture = Http().bindAndHandleAsync(requestQueue, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")

  def translate(cpspc: Any): HttpResponse = cpspc match {
    case Pong() =>
      HttpResponse(entity = "PONG!")
  }

  def send(msg: Any) = {
    responsesQueue.take() success translate(msg)
  }

  def receive() = {
    requestsQueue.take() match {
      case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
        //          println("CM retrieved ping, responsesQueue size: ",responsesQueue.size)
        Ping()(null)
      case m =>
      //          println("CM retrieved",m)
    }
  }

//  ponger(c)
}
