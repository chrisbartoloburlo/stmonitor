package monitor.parser

import monitor.model._

import scala.language.postfixOps
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

class STParser extends StandardTokenParsers {
  lexical.reserved += ("rec", "end")

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

  def receive: Parser[SendStatement] = ("?" ~> ident) ~ ("(" ~> types <~ ")") ~ ("[" ~> probBoundary <~ "]") ~ opt("." ~> sessionType) ^^ {
    case l ~ t ~ p ~ None =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, p, End())
    case l ~ t ~ p ~ cT =>
      SendStatement(l, l+"_"+getAndIncrementIDCounter, t, p, cT.get)
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

  def send: Parser[ReceiveStatement] = ("!" ~> ident) ~ ("(" ~> types <~ ")") ~ ("[" ~> probBoundary <~ "]") ~ opt("." ~> sessionType) ^^ {
    case l ~ t ~ p ~ None =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, p, End())
    case l ~ t ~ p ~ cT =>
      ReceiveStatement(l, l+"_"+getAndIncrementIDCounter, t, p, cT.get)
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

  def probBoundary: Parser[Boundary] = stringLit ^^ {
    b =>
      val boundary = b.replace(" ", "")
      if (boundary.charAt(0) == '*' && boundary.charAt(boundary.length - 1) == '*')
        if (b.length == 1) {
          Boundary(lessThan = false, 0, greaterThan = false)
        } else { throw new Exception("* must only be for either the lower or upper boundary") }
      else if(boundary.charAt(0)=='*' && boundary.charAt(boundary.length - 1) != '*') {
        Boundary(lessThan = false, boundary.substring(2).toDouble, greaterThan = true)
      } else if(boundary.charAt(boundary.length-1)=='*' && boundary.charAt(0)!='*') {
        Boundary(lessThan = true, boundary.substring(0, boundary.length - 2).toDouble, greaterThan = false)
      } else {
        Boundary(lessThan = true, boundary.toDouble, greaterThan = true)
      }
  }

//  def probBoundary: Parser[Boundary] = repsep(stringLit, ",") ^^ {
//    boundary =>
//      if(boundary.head=="*" && boundary.last=="*")
//        throw new Exception("* must either be for the lower or the upper boundary only")
//      if(boundary.head=="*" ) {
//        Boundary(lessThan = false, boundary(1).toDouble, greaterThan = true)
//      }
//      if(boundary.last=="*") {
//        Boundary(lessThan = true, boundary.head.toDouble, greaterThan = true)
//      }
//      Boundary(lessThan = true, boundary.head.toDouble, greaterThan = true)
//  }

//  def probBoundary: Parser[Boundary] = opt("*" ~ ",") ~ stringLit ~ opt("," ~ "*") ^^ {
//    case None ~ p ~ None =>
//      println("prob: "+p)
//      Boundary(lessThan = true, p, greaterThan = true)
//    case _ ~ p ~ None =>
//      println("prob: "+p)
//      Boundary(lessThan = false, p, greaterThan = true)
//    case None ~ p ~ _ =>
//      println("prob: "+p)
//      Boundary(lessThan = true, p, greaterThan = false)
//    case _ ~ _ ~ _ =>
//      throw new Exception("* must either be for the lower or the upper boundary only")
//  }

  //  def typ: Parser[String] = "String" | "Int" | "Bool" ^^ (t => t)

  def parseAll[T](p: Parser[T], in: String): ParseResult[T] = {
    val assertionPattern = """\[(.*?)\]""".r
    phrase(p)(new lexical.Scanner(assertionPattern.replaceAllIn(in, "[\""+_.group(1).replace("\"", "\\\\\"")+"\"]")))
  }
}