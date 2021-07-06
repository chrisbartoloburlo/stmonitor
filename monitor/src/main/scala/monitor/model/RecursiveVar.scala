package monitor.model

case class RecursiveVar(name: String, continuation: Statement) extends Statement
