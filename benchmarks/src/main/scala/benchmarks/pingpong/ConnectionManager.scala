package benchmarks.pingpong

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer

import scala.concurrent.{Future, Promise}
import scala.collection.mutable
import java.util.concurrent.LinkedBlockingQueue

class ConnectionManager {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
//  var queue = mutable.Queue[(HttpRequest, Promise[HttpResponse])]()
  var requestsQueue = new LinkedBlockingQueue[HttpRequest]()
  var responsesQueue = mutable.Queue[Promise[HttpResponse]]()

  val requestQueue: HttpRequest => Future[HttpResponse] = {
    case ping @ HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      val p = Promise[HttpResponse]()
      requestsQueue.put(ping)
      responsesQueue.enqueue(p)
      println("CM queued ping")
      p.future
  }

  val bindingFuture = Http().bindAndHandleAsync(requestQueue, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
//  StdIn.readLine() // let it run until user presses return

  def setup(): Unit ={

  }

  def receive(): Any = {
    requestsQueue.take() match {
      case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
        println("CM retrieved ping")
        Ping()(null)
      case m =>
        println("CM retrieved",m)
    }
  }

  def send(msg: Any): Any = {
    responsesQueue.dequeue() success translate(msg)
  }

  def translate(cpspc: Any): HttpResponse = cpspc match {
    case Pong() =>
      HttpResponse(entity = "PONG!")
  }

  def close(): Unit = {
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
