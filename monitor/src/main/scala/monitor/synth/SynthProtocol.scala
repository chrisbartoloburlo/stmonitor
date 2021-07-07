package monitor.synth

import monitor.interpreter.STInterpreter
import monitor.model._


class SynthProtocol(sessionTypeInterpreter: STInterpreter, path: String) {
//  private val protocol = new PrintWriter(new File(path+"/CPSPc.scala"))
  private val protocol = new StringBuilder()
  private var importIn = false
  private var importOut = false

  def getProtocol(): StringBuilder ={
    protocol
  }

  def init(preamble: String): Unit = {
    if (preamble!="") protocol.append(preamble+"\n")
    protocol.append("import lchannels.$\n")
  }

  def handleSendChoice(label: String): Unit ={
    protocol.append("sealed abstract class "+label+"\n")
  }

  def handleReceiveChoice(label: String): Unit={
    protocol.append("sealed abstract class "+label+"\n")
  }

  def handleSend(statement: SendStatement, isCurUnique: Boolean, nextStatement: Statement, isNextUnique: Boolean, label: String): Unit = {
    if(isCurUnique){
      protocol.append("case class "+statement.label+"(")
    } else {
      protocol.append("case class "+statement.statementID+"(")
    }
    handleParam(statement)
    protocol.append(")")
    handleSendNextCase(nextStatement, isNextUnique)

    label match {
      case null =>
        protocol.append("\n")
      case _ =>
        protocol.append(" extends "+label+"\n")
    }
  }

  def handleReceive(statement: ReceiveStatement, isCurUnique: Boolean, nextStatement: Statement, isNextUnique: Boolean, label: String): Unit = {
    if(isCurUnique){
      protocol.append("case class "+statement.label+"(")
    } else {
      protocol.append("case class "+statement.statementID+"(")
    }
    handleParam(statement)
    protocol.append(")")
    handleReceiveNextCase(nextStatement, isNextUnique)

    label match {
      case null =>
        protocol.append("\n")
      case _ =>
        protocol.append(" extends "+label+"\n")
    }
  }

  def handleParam(statement: Statement): Unit = {
    statement match {
      case s @ SendStatement(_, _, _, _, _) =>
        for(t <- s.types){
          protocol.append(t._1+": "+t._2)
          if(!(t == s.types.last)){
            protocol.append(", ")
          }
        }
      case s @ ReceiveStatement(_, _, _, _, _) =>
        for(t <- s.types){
          protocol.append(t._1+": "+t._2)
          if(!(t == s.types.last)){
            protocol.append(", ")
          }
        }
    }
  }

  def handleSendNextCase(statement: Statement, isUnique: Boolean): Unit ={
    statement match {
      case s @ SendStatement(_, _, _, _, _) =>
        if(isUnique){
          protocol.append("(val cont: In["+s.label+"])")
        } else {
          protocol.append("(val cont: In["+s.statementID+"])")
        }
        importIn = true
      case s @ SendChoiceStatement(_, _) =>
        protocol.append("(val cont: In["+s.label+"])")
        importIn = true
      case s @ ReceiveStatement(_, _, _, _, _) =>
        if(isUnique){
          protocol.append("(val cont: Out["+s.label+"])")
        } else {
          protocol.append("(val cont: Out["+s.statementID+"])")
        }
        importOut = true
      case s @ ReceiveChoiceStatement(_, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
        importOut = true
      case s @ RecursiveVar(_, _) =>
        handleSendNextCase(sessionTypeInterpreter.getRecursiveVarScope(s).recVariables(s.name), isUnique)
      case s @ RecursiveStatement(_, _) =>
        handleSendNextCase(s.body, isUnique)
      case _ =>

    }
  }

  def handleReceiveNextCase(statement: Statement, isUnique: Boolean): Unit ={
    statement match {
      case s @ SendStatement(_, _, _, _, _) =>
        if(isUnique){
          protocol.append("(val cont: Out["+s.label+"])")
        } else {
          protocol.append("(val cont: Out["+s.statementID+"])")
        }
        importOut = true
      case s @ SendChoiceStatement(_, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
        importOut = true
      case s @ ReceiveStatement(_, _, _, _, _) =>
        if(isUnique){
          protocol.append("(val cont: In["+s.label+"])")
        } else {
          protocol.append("(val cont: In["+s.statementID+"])")
        }
        importIn = true
      case s @ ReceiveChoiceStatement(_, _) =>
        protocol.append("(val cont: In["+s.label+"])")
        importIn = true
      case s @ RecursiveVar(_, _) =>
        handleReceiveNextCase(sessionTypeInterpreter.getRecursiveVarScope(s).recVariables(s.name), isUnique)
      case s @ RecursiveStatement(_, _) =>
        handleReceiveNextCase(s.body, isUnique)
      case _ =>

    }
  }

  def end(): Unit ={
    if (importIn && importOut) protocol.replace(protocol.indexOf("$"), protocol.indexOf("$")+1, "{In, Out}")
    else if (importIn) protocol.replace(protocol.indexOf("$"), protocol.indexOf("$")+1, "In")
    else if (importOut) protocol.replace(protocol.indexOf("$"), protocol.indexOf("$")+1, "Out")
    else protocol.replace(protocol.indexOf("$"), protocol.indexOf("$")+1, "_")
  }
}
