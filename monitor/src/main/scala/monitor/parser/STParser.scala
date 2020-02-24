package monitor.parser

import monitor.model._

import scala.collection.mutable
import scala.language.postfixOps
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

class STParser extends StandardTokenParsers {
  lexical.reserved += ("rec", "end", "String", "Int", "Boolean")

  lexical.delimiters += ("?", "!", "&", "+", "(", ")", "{", "}", ",", ":", "=", ".", "[", "]")

  private var globalVar = new mutable.HashMap[String, String]

  private var sendChoiceCounter: Int = 0
  private var receiveChoiceCounter: Int = 0

  def getGlobalVar: mutable.HashMap[String, String] = {
    globalVar
  }

  def sessionType: Parser[SessionType] = (ident ~> "=") ~ repsep(statement, ".") ^^ {
    case i ~ s =>
      new SessionType(i, s)
  }

  def statement: Parser[Statement] = positioned( receive | send | receiveChoice | sendChoice | recursive | recursiveVar ) ^^ { a => a }

  def receive: Parser[ReceiveStatement] = ("?" ~> ident) ~ ("(" ~> types <~ ")") ~ opt("[" ~> stringLit <~ "]") ~ opt(("." ~ ">" ~> ident <~ "(") ~ repsep(statement, ".") ~ "," ~ repsep(statement, ".") <~ ")") ^^ {
    case l ~ t ~ None ~ None =>
      ReceiveStatement(l, t, null, null)
    case l ~ t ~ c ~ None =>
      ReceiveStatement(l, t, c.get, null)
    case l ~ t ~ c ~ b =>
      ReceiveStatement(l, t, c.get, Branch(b.get._1._1._1, b.get._1._1._2, b.get._2))
    case _ ~ _ ~ None ~ _ =>
      throw new Exception("Branching without condition")
  }

  def receiveChoice: Parser[ReceiveChoiceStatement] = "&" ~ "{" ~> (repsep(repsep(statement, "."), ",") <~ "}") ^^ {
    cN =>
      for (s <- cN) {
        s.head match {
          case _: ReceiveStatement =>
          case _ =>
            throw new Exception("& must be followed with ?")
        }
      }
      ReceiveChoiceStatement(f"ExternalChoice${receiveChoiceCounter+=1;receiveChoiceCounter.toString}", cN)
  }

  def send: Parser[SendStatement] = ("!" ~> ident) ~ ("(" ~> types <~ ")") ~ opt("[" ~> stringLit <~ "]") ~ opt(("." ~ ">" ~> ident <~ "(") ~ repsep(statement, ".") ~ "," ~ repsep(statement, ".") <~ ")") ^^ {
    case l ~ t ~ None ~ None =>
      SendStatement(l, t, null, null)
    case l ~ t ~ c ~ None =>
      SendStatement(l, t, c.get, null)
    case l ~ t ~ c ~ b =>
      println(b.get._1._1._1, b.get._1._1._2, b.get._2)
      SendStatement(l, t, c.get, Branch(b.get._1._1._1, b.get._1._1._2, b.get._2))
    case _ ~ _ ~ None ~ _ =>
      throw new Exception("Branching without condition")
  }

  def sendChoice: Parser[SendChoiceStatement] = "+" ~ "{" ~> (repsep(repsep(statement, "."), ",") <~ "}") ^^ {
    cN =>
      for (s <- cN) {
        s.head match {
          case _: SendStatement =>
          case _ =>
            throw new Exception("+ must be followed with !")
        }
      }
      SendChoiceStatement(f"InternalChoice${sendChoiceCounter+=1;sendChoiceCounter.toString}", cN)
  }

  def recursive: Parser[RecursiveStatement] = ("rec" ~> recursiveVar <~ ".") ~ ("(" ~> repsep(statement, ".") <~ ")") ^^ {
    case i ~ sN =>
      RecursiveStatement(i, sN)
  }

  def recursiveVar: Parser[RecursiveVar] = ident ^^ (i => RecursiveVar(i))

  def types: Parser[Map[String, String]] = repsep(typDef, ",") ^^ {
    _ toMap
  }

  def typDef: Parser[(String, String)] = (ident <~ ":") ~ typ ^^ {
    case a ~ b =>
      (a, b)
  }

  def typ: Parser[String] = "String" | "Int" | "Bool" ^^ (t => t)

  def parseAll[T](p: Parser[T], in: String): ParseResult[T] = {
    val assertionPattern = """\[(.*?)\]""".r
    phrase(p)(new lexical.Scanner(assertionPattern.replaceAllIn(in.replace("\"", "\\\""), "[\""+_.group(1)+"\"]")))
  }
}