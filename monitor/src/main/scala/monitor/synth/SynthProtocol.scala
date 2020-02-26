package monitor.synth

import java.io.{File, PrintWriter}

import monitor.interpreter.STInterpreter
import monitor.model._

class SynthProtocol(sessionTypeInterpreter: STInterpreter, path: String) {
//  private val protocol = new PrintWriter(new File(path+"/CPSPc.scala"))
  private val protocol = new StringBuilder()

  def getProtocol(): StringBuilder ={
    protocol
  }

  def init(): Unit = {
    protocol.append("import lchannels.{In, Out}\n")
  }

  def handleSendChoice(label: String): Unit ={
    protocol.append("sealed abstract class "+label+"\n")
  }

  def handleReceiveChoice(label: String): Unit={
    protocol.append("sealed abstract class "+label+"\n")
  }

  def handleSend(statement: SendStatement, nextStatement: Statement, label: String): Unit = {
    protocol.append("case class "+statement.label+"(")
    handleParam(statement)
    protocol.append(")")
    handleSendNextCase(nextStatement)

    label match {
      case null =>
        protocol.append("\n")
      case _ =>
        protocol.append(" extends "+label+"\n")
    }
  }

  def handleReceive(statement: ReceiveStatement, nextStatement: Statement, label: String): Unit = {
    protocol.append("case class "+statement.label+"(")
    handleParam(statement)
    protocol.append(")")
    handleReceiveNextCase(nextStatement)

    label match {
      case null =>
        protocol.append("\n")
      case _ =>
        protocol.append(" extends "+label+"\n")
    }
  }

  def handleParam(statement: Statement): Unit = {
    statement match {
      case s @ SendStatement(_, _, _, _) =>
        for(t <- s.types){
          protocol.append(t._1+": "+t._2)
          if(!(t == s.types.last)){
            protocol.append(", ")
          }
        }
      case s @ ReceiveStatement(_, _, _, _) =>
        for(t <- s.types){
          protocol.append(t._1+": "+t._2)
          if(!(t == s.types.last)){
            protocol.append(", ")
          }
        }
    }
  }

  def handleSendNextCase(statement: Statement): Unit ={
    statement match {
      case s @ SendStatement(_, _, _, _) =>
        protocol.append("(val cont: In["+s.label+"])")
      case s @ SendChoiceStatement(_, _) =>
        protocol.append("(val cont: In["+s.label+"])")
      case s @ ReceiveStatement(_, _, _, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
      case s @ ReceiveChoiceStatement(_, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
      case s @ RecursiveVar(_, _) =>
        handleSendNextCase(sessionTypeInterpreter.getRecursiveVarScope(s).recVariables(s.name))
      case _ =>

    }
  }

  def handleReceiveNextCase(statement: Statement): Unit ={
    statement match {
      case s @ SendStatement(_, _, _, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
      case s @ SendChoiceStatement(_, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
      case s @ ReceiveStatement(_, _, _, _) =>
        protocol.append("(val cont: In["+s.label+"])")
      case s @ ReceiveChoiceStatement(_, _) =>
        protocol.append("(val cont: In["+s.label+"])")
      case s @ RecursiveVar(_, _) =>
        handleReceiveNextCase(sessionTypeInterpreter.getRecursiveVarScope(s).recVariables(s.name))
      case _ =>

    }
  }

  def end(): Unit ={
    protocol.append("case class MonStart()\n")
  }
}
