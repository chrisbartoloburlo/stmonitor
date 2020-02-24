package monitor.model

case class ReceiveStatement(label: String, types: Map[String, String], condition: String, continuation: Statement) extends Statement