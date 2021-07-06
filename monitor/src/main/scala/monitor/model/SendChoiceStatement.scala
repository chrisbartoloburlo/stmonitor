package monitor.model

case class SendChoiceStatement(label: String, choices: List[Statement]) extends Statement
