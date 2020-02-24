package monitor.model

case class SendChoiceStatement(label: String, choices: List[List[Statement]]) extends Statement
