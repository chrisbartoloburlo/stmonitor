package monitor.parser

import monitor.model._

import scala.language.postfixOps
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

class STParser extends StandardTokenParsers {
  lexical.reserved += ("rec", "end", "String", "Int", "Boolean")

  lexical.delimiters += ("?", "!", "&", "+", "(", ")", "{", "}", ",", ":", "=", ".", "[", "]")

  private var sendChoiceCounter: Int = 0
  private var receiveChoiceCounter: Int = 0
  private var statementIDCounter: Int = 0

  def getAndIncrementIDCounter(): Int = {
    statementIDCounter+=1
    statementIDCounter
  }

  def sessionTypeVar: Parser[SessionType] = (ident ~> "=") ~ sessionType ^^ {
    case i ~ t =>
      new SessionType(i, t)
  }

  def sessionType: Parser[Statement] = positioned (choice | receive | send | recursive | recursiveVar | end) ^^ {a=>a}

  def choice: Parser[Statement] = positioned( receiveChoice | sendChoice ) ^^ {a=>a}

  def receive: Parser[ReceiveStatement] = ("?" ~> ident) ~ ("(" ~> types <~ ")") ~ ("[" ~> stringLit <~ "]") ~ opt("." ~> sessionType) ^^ {
    case l ~ t ~ p ~ None =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, End())
    case l ~ t ~ p ~ cT =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, cT.get)
    case l ~ t ~ p~ None =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, End())
    case l ~ t ~ p ~ cT =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, cT.get)
  }

  def receiveChoice: Parser[ReceiveChoiceStatement] = "&" ~ "{" ~> (repsep(sessionType, ",") <~ "}") ^^ {
    cN =>
      for (s <- cN) {
        s match {
          case _: ReceiveStatement =>
          case _ =>
            throw new Exception("& must be followed with ?")
        }
      }
      ReceiveChoiceStatement(f"ExternalChoice${receiveChoiceCounter+=1;receiveChoiceCounter.toString}", cN)
  }

  def send: Parser[SendStatement] = ("!" ~> ident) ~ ("(" ~> types <~ ")") ~ ("[" ~> stringLit <~ "]") ~ opt("." ~> sessionType) ^^ {
    case l ~ t ~ p ~ None =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, End())
    case l ~ t ~ p ~ cT =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, cT.get)
    case l ~ t ~ p ~ None =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, End())
    case l ~ t ~ p ~ cT =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, p.toDouble, cT.get)
  }

  def sendChoice: Parser[SendChoiceStatement] = "+" ~ "{" ~> (repsep(sessionType, ",") <~ "}") ^^ {
    cN =>
      for (s <- cN) {
        s match {
          case _: SendStatement =>
          case _ =>
            throw new Exception("+ must be followed with !")
        }
      }
      SendChoiceStatement(f"InternalChoice${sendChoiceCounter+=1;sendChoiceCounter.toString}", cN)
  }

  def recursive: Parser[RecursiveStatement] = ("rec" ~> ident <~ ".") ~ ("(" ~> sessionType <~ ")") ^^ {
    case i ~ cT =>
      RecursiveStatement(i, cT)
  }

  def recursiveVar: Parser[RecursiveVar] = ident ~ opt("." ~> sessionType) ^^ {
    case i ~ None =>
      RecursiveVar(i, End())
    case i ~ cT =>
      RecursiveVar(i, cT.get)
  }

  def types: Parser[Map[String, String]] = repsep(typDef, ",") ^^ {
    _ toMap
  }

  def typDef: Parser[(String, String)] = (ident <~ ":") ~ typ ^^ {
    case a ~ b =>
      (a, b)
  }

  def end: Parser[End] = ("" | "end") ^^ (_ => End())

  def typ: Parser[String] = "String" | "Int" | "Bool" ^^ (t => t)

  def parseAll[T](p: Parser[T], in: String): ParseResult[T] = {
    val assertionPattern = """\[(.*?)\]""".r
    phrase(p)(new lexical.Scanner(assertionPattern.replaceAllIn(in, "[\""+_.group(1).replace("\"", "\\\\\"")+"\"]")))
  }
}