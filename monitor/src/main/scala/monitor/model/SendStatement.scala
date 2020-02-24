package monitor.model

case class SendStatement(label: String, types: Map[String, String], condition: String, continuation: Statement) extends Statement
