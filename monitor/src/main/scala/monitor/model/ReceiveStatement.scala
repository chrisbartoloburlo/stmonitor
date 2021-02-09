package monitor.model

case class ReceiveStatement(label: String, statementID: String, types: Map[String, String], probability: Double, continuation: Statement) extends Statement