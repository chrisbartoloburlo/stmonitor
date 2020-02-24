package monitor.synth

import java.io.{File, PrintWriter}

import monitor.interpreter.STInterpreter
import monitor.model._

class SynthProtocol(sessionTypeInterpreter: STInterpreter, path: String) {
  private val protocol = new PrintWriter(new File(path+"/CPSPc.scala"))

  def init(): Unit = {
    protocol.write("import lchannels.{In, Out}\n")
  }

  def handleSendChoice(label: String): Unit ={
    protocol.write("sealed abstract class "+label+"\n")
  }

  def handleReceiveChoice(label: String): Unit={
    protocol.write("sealed abstract class "+label+"\n")
  }

  def handleSend(statement: SendStatement, nextStatement: Statement, label: String): Unit = {
    protocol.write("case class "+statement.label+"(")
    handleParam(statement)
    protocol.write(")")
    handleSendNextCase(nextStatement)

    label match {
      case null =>
        protocol.write("\n")
      case _ =>
        protocol.write(" extends "+label+"\n")
    }
  }

  def handleReceive(statement: ReceiveStatement, nextStatement: Statement, label: String): Unit = {
    protocol.write("case class "+statement.label+"(")
    handleParam(statement)
    protocol.write(")")
    handleSendNextCase(nextStatement)

    label match {
      case null =>
        protocol.write("\n")
      case _ =>
        protocol.write(" extends "+label+"\n")
    }
  }

  def handleParam(statement: Statement): Unit = {
    statement match {
      case s @ SendStatement(_, _, _, _) =>
        for(t <- s.types){
          protocol.write(t._1+": "+t._2)
          if(!(t == s.types.last)){
            protocol.write(", ")
          }
        }
      case s @ ReceiveStatement(_, _, _, _) =>
        for(t <- s.types){
          protocol.write(t._1+": "+t._2)
          if(!(t == s.types.last)){
            protocol.write(", ")
          }
        }
    }
  }

  def handleSendNextCase(statement: Statement): Unit ={
    statement match {
      case s @ SendStatement(_, _, _, _) =>
        protocol.write("(val cont: Out["+s.label+"])")
      case s @ SendChoiceStatement(_, _) =>
        protocol.write("(val cont: Out["+s.label+"])")
      case s @ ReceiveStatement(_, _, _, _) =>
        protocol.write("(val cont: Out["+s.label+"])")
      case s @ ReceiveChoiceStatement(_, _) =>
        protocol.write("(val cont: Out["+s.label+"])")
      case s @ RecursiveVar(_, _) =>
        handleSendNextCase(sessionTypeInterpreter.getRecursiveVarScope(s).recVariables(s.name))
      case _ =>

    }
  }

  def end(): Unit ={
    protocol.write("case class MonStart()\n")
    protocol.close()
  }
}
