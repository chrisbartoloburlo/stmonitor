package monitor.model

case class ReceiveChoiceStatement(label: String, choices: List[List[Statement]]) extends Statement
