package monitor.synth

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
//    CODE FOR GENERATING UNIQUE PROTOCOL CLASSES
//    var nonUniqueScopes = new ListBuffer[String]
//    for(scope <- sessionTypeInterpreter.getScopes){
//      if (!scope._2.isUnique && !nonUniqueScopes.contains(scope._2.name)) {
//        protocol.append("case class "+ scope._2.name + "(")
//        for(variable <- scope._2.variables){
//          protocol.append(variable._1+": "+variable._2._2)
//          if(!(variable == scope._2.variables.last)){
//            protocol.append(", ")
//          }
//        }
//        nonUniqueScopes += scope._2.name
//        protocol.append(")\n")
//      }
//    }
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
      case s @ SendChoiceStatement(_, _) =>
        protocol.append("(val cont: In["+s.label+"])")
      case s @ ReceiveStatement(_, _, _, _, _) =>
        if(isUnique){
          protocol.append("(val cont: Out["+s.label+"])")
        } else {
          protocol.append("(val cont: Out["+s.statementID+"])")
        }
      case s @ ReceiveChoiceStatement(_, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
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
      case s @ SendChoiceStatement(_, _) =>
        protocol.append("(val cont: Out["+s.label+"])")
      case s @ ReceiveStatement(_, _, _, _, _) =>
        if(isUnique){
          protocol.append("(val cont: In["+s.label+"])")
        } else {
          protocol.append("(val cont: In["+s.statementID+"])")
        }
      case s @ ReceiveChoiceStatement(_, _) =>
        protocol.append("(val cont: In["+s.label+"])")
      case s @ RecursiveVar(_, _) =>
        handleReceiveNextCase(sessionTypeInterpreter.getRecursiveVarScope(s).recVariables(s.name), isUnique)
      case s @ RecursiveStatement(_, _) =>
        handleReceiveNextCase(s.body, isUnique)
      case _ =>

    }
  }

  def end(): Unit ={
//    protocol.append("case class MonStart()\n")
  }
}
