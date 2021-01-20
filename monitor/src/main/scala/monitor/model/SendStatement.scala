package monitor.model

case class SendStatement(label: String, statementID: String, types: Map[String, String], condition: String, continuation: Statement) extends Statement
