package examples.pingpong

import monitor.util.ConnectionManager

class ClientConnectionManager(port: Int) extends ConnectionManager {
//  TODO
  override def setup(): Unit = {
//    Wait for a connection from a client using (if necessary) the port
  }

  override def receive(): Any = {
//    case /ping => Ping()
//    case /quit => Quit()
  }

  override def send(x: Any): Unit = {
//    case Pong() => Pong()
  }

  override def close(): Unit = {
//    close sockets (if necessary)
  }
}
