package monitor.model

case class SendStatement(label: String, statementID: String, types: Map[String, String], probBoundary: Boundary, continuation: Statement) extends Statement
