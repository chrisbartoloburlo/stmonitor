package monitor.util

abstract class ConnectionManager {
  def setup(): Unit

  def receive(): Any

  def send(x: Any): Unit

  def close(): Unit
}
