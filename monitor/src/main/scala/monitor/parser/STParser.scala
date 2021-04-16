package monitor.parser

import monitor.model._

import scala.language.postfixOps
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

class STParser extends StandardTokenParsers {
  lexical.reserved += ("rec", "end")
//  "String", "Int", "Boolean"

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

  def receive: Parser[SendStatement] = ("?" ~> ident) ~ ("(" ~> types <~ ")") ~ opt("[" ~> stringLit <~ "]") ~ opt("." ~> sessionType) ^^ {
    case l ~ t ~ None ~ None =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, null, End())
    case l ~ t ~ None ~ cT =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, null, cT.get)
    case l ~ t ~ c ~ None =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, c.get, End())
    case l ~ t ~ c ~ cT =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, c.get, cT.get)
  }

  def receiveChoice: Parser[SendChoiceStatement] = "&" ~ "{" ~> (repsep(sessionType, ",") <~ "}") ^^ {
    cN =>
      for (s <- cN) {
        s match {
          case _: SendStatement =>
          case _ =>
            throw new Exception("& must be followed with ?")
        }
      }
      SendChoiceStatement(f"ExternalChoice${receiveChoiceCounter+=1;receiveChoiceCounter.toString}", cN)
  }

  def send: Parser[ReceiveStatement] = ("!" ~> ident) ~ ("(" ~> types <~ ")") ~ opt("[" ~> stringLit <~ "]") ~ opt("." ~> sessionType) ^^ {
    case l ~ t ~ None ~ None =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, null, End())
    case l ~ t ~ None ~ cT =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, null, cT.get)
    case l ~ t ~ c ~ None =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, c.get, End())
    case l ~ t ~ c ~ cT =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, c.get, cT.get)
  }

  def sendChoice: Parser[ReceiveChoiceStatement] = "+" ~ "{" ~> (repsep(sessionType, ",") <~ "}") ^^ {
    cN =>
      for (s <- cN) {
        s match {
          case _: ReceiveStatement =>
          case _ =>
            throw new Exception("+ must be followed with !")
        }
      }
      ReceiveChoiceStatement(f"InternalChoice${sendChoiceCounter+=1;sendChoiceCounter.toString}", cN)
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

  def typDef: Parser[(String, String)] = (ident <~ ":") ~ ident ^^ {
    case a ~ b =>
      (a, b)
  }

  def end: Parser[End] = ("" | "end") ^^ (_ => End())

//  def typ: Parser[String] = "String" | "Int" | "Boolean" ^^ (t => t)

  def parseAll[T](p: Parser[T], in: String): ParseResult[T] = {
    val assertionPattern = """\[(.*?)\]""".r
    phrase(p)(new lexical.Scanner(assertionPattern.replaceAllIn(in, "[\""+_.group(1).replace("\"", "\\\\\"")+"\"]")))
  }
}