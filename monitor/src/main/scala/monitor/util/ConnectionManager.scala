package monitor.util

/**
 * Base class for connection management between a monitor and an interacting component.
 *
 * Extend this class for writing a connection manager that translates between custom message formats and the generated CPSPc.scala for a given session type.
 */
abstract class ConnectionManager {
  /**
   * This method is invoked by the monitor once it starts executing to setup the connection between the monitor itself and the component.
   */
  def setup(): Unit

  /**
   * This method is invoked by the monitor for the internal choice +{!Label(payload)[assertion].S, ...} to retrieve the message sent by the component.
   *
   * @return The CPSP class representing the message sent by the component.
   */
  def receive(): Any

  /**
   * This method is invoked by the montor for the external choice &{?Label(payload)[assertion].S, ...} to send a message to the component.
   *
   * @param x The CPSP class representing the message to be sent to the component.
   */
  def send(x: Any): Unit

  /**
   * This method is invoked by the monitor before it terminates either due to a violation or because the session ends.
   */
  def close(): Unit
}
