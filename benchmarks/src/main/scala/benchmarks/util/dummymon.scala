package benchmarks.util

import akka.actor.{ Actor, ActorContext, ActorRef, ActorSystem, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

class dummymon(listenPort: Int, serverAddr: String, serverPort: Int) extends Runnable {
  override def run() {
//    if (args.length != 3) {
//      System.err.println("Usage: java " + this.getClass().getName().split('$').head
//                         + " <listenPort> <serverAddr> <serverPort>")
//      System.exit(1)
//    }
    
//    val listenPort = args(0).toInt
//    val serverAddr = args(1)
//    val serverPort = args(2).toInt

    val system = ActorSystem("DummyMonitorSystem")
    val clientSide = system.actorOf(Props(classOf[ClientSide],
                                          listenPort, serverAddr, serverPort),
                                    "ClientSide")

    import scala.concurrent.Await
    import scala.concurrent.duration._
    Await.ready(system.whenTerminated, 30.days)
  }
}

object util {
  def forwarder(self: ActorRef, peer: ActorRef, connection: ActorRef)
               (implicit context: ActorContext): Actor.Receive = {
    case data: ByteString =>
      connection ! Tcp.Write(data)
    case Tcp.Received(data) =>
      peer ! data
    case _: Tcp.ConnectionClosed =>
       println("*** TCP connection closed!")
      peer ! "close"
    case "close" =>
       println("*** Peer's connection closed!")
      connection ! Tcp.Close
      context.system.terminate()
    case Tcp.CommandFailed(w: Tcp.Write) =>
      println("*** Write failed!")
    case "connect failed" =>
      connection ! Tcp.Close
      context.system.terminate()
  }
}

object ClientSide {
  def props(listenPort: Int, serverAddr: String, serverPort: Int) =
    Props(classOf[ClientSide], listenPort, serverAddr, serverPort)
}

class ClientSide(port: Int, serverAddr: String, serverPort: Int) extends Actor {

  import Tcp._
  import context.system

  // println("*** ClientSide running")
  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", port))

  def receive = {
    case b @ Bound(localAddress) =>
      context.parent ! b

    case CommandFailed(_: Bind) => context.stop(self)

    case c @ Connected(remote, local) =>
      // println{"*** Connected!"}
      val connection = sender()
      connection ! Register(self)
      val serverSide = system.actorOf(Props(classOf[ServerSide], serverAddr, serverPort, self), "ServerSide")
      context.become(util.forwarder(self, serverSide, connection))
  }
}

object ServerSide {
  def props(serverAddr: String, serverPort: Int, clientSide: ActorRef) =
    Props(classOf[ServerSide], serverAddr, serverPort, clientSide)
}

class ServerSide(serverAddr: String, serverPort: Int, clientSide: ActorRef) extends Actor {

  import Tcp._
  import context.system

  println(s"*** ServerSide running --- Conneting to: ${serverAddr}:${serverPort}")
  IO(Tcp) ! Connect(new InetSocketAddress(serverAddr, serverPort))

  def receive = {
    case CommandFailed(_: Connect) =>
       println("*** Connection failed!")
      clientSide ! "connect failed"
      context.stop(self)

    case Connected(remote, local) =>
      val connection = sender()
      connection ! Register(self)
      context.become(util.forwarder(self, clientSide, connection))
  }
}
