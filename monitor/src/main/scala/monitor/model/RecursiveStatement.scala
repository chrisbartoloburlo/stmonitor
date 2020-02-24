package monitor.model

case class RecursiveStatement(label: RecursiveVar, body: List[Statement]) extends Statement
