package monitor.model

import scala.util.parsing.input.Positional

class Expr extends Positional

case class Number(value: Int) extends Expr

case class Identifier(scope: String, name: String) extends Expr

case class Condition(op: String, var left: Expr, right: Expr) extends Expr
