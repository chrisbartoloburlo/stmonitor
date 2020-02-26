package monitor.synth

import java.io.{File, PrintWriter}

import monitor.interpreter.STInterpreter
import monitor.model._

class SynthMon(sessionTypeInterpreter: STInterpreter, path: String) {
//  private val mon = new PrintWriter(new File(path+"/Mon.scala"))
  private val mon = new StringBuilder()

  def getMon(): StringBuilder = {
    mon
  }

  private var first = true

  def startInit(statement: Statement): Unit = {
    mon.append("import akka.actor._\nimport lchannels.{In, Out}\nimport scala.concurrent.ExecutionContext\nimport scala.concurrent.duration.Duration\nclass Mon(Internal: ")

    statement match {
      case ReceiveStatement(label, _, _, _) =>
        mon.append("Out["+label+"])")
      case SendStatement(label, _, _, _) =>
        mon.append("In["+label+"])")
      case ReceiveChoiceStatement(label, _) =>
        mon.append("Out["+label+"])")
      case SendChoiceStatement(label, _) =>
        mon.append("In["+label+"])")
    }

    mon.append("(implicit ec: ExecutionContext, timeout: Duration) extends Actor {\n")
    mon.append("  object payloads {\n")
  }

  def handlePayloads(label: String, types: Map[String, String]): Unit ={
    mon.append("\t\tobject "+label+" {\n")
    for(typ <- types){
      mon.append("\t\t\tvar "+typ._1+": "+typ._2+" = _\n")
    }
    mon.append("\t\t}\n")
  }

  def endInit(): Unit = {
    mon.append("\t}\n")
    mon.append("  def receive: Receive = {\n    case MonStart =>\n      println(\"[Mon] Monitor started\")\n      println(\"[Mon] Setting up connection manager\")\n")
    mon.append("      val cm = new ConnectionManager()\n      cm.setup()\n")
  }

  def handleSend(statement: SendStatement, nextStatement: Statement): Unit = {
    if(first){
      mon.append("      send"+statement.label+"(Internal, cm)\n      cm.close()\n  }\n")
      first = false
    }

    try {
      mon.append("  def send"+statement.label+"(internal: In["+sessionTypeInterpreter.getBranchLabel(statement)+"], External: ConnectionManager): Any = {\n")
    } catch {
      case _: Throwable =>
        mon.append("  def send"+statement.label+"(internal: In["+statement.label+"], External: ConnectionManager): Any = {\n")
    }

    mon.append("    internal ? {\n")
    mon.append("      case msg @ "+statement.label+"(")
    addParameters(statement.types)
    mon.append(") =>\n")

    if(statement.condition != null){
      handleCondition(statement.condition, statement.label)
      mon.append("External.send(msg)\n")
      handleSendNextCase(statement, nextStatement)
      mon.append("        } else {\n")
    } else {
      mon.append("        External.send(msg)\n")
      handleSendNextCase(statement, nextStatement)
    }
    if(statement.condition != null){
      mon.append("        }\n")
    }
    mon.append("    }\n  }\n")
  }

  private def storeValue(types: Map[String, String], checkCondition: Boolean, curStatementScope: String): Unit = {
    for((name, _) <- types) {
      val (varScope, (global, _)) = sessionTypeInterpreter.getVarInfo(name, curStatementScope)
      if(global) {
        if(checkCondition){
          mon.append("\t\t\t\t\t\tpayloads."+varScope+"."+name+" = msg."+name+"\n")
        } else {
          mon.append("\t\t\t\t\tpayloads."+varScope+"."+name+" = msg."+name+"\n")
        }
      }
    }
  }

  @scala.annotation.tailrec
  private def handleSendNextCase(currentStatement: SendStatement, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tsend"+sendStatement.label+"(msg.cont, External)\n")
        } else {
          mon.append("\t\t\t\t\tsend"+sendStatement.label+"(msg.cont, External)\n")
        }

      case sendChoiceStatement: SendChoiceStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tsend"+sendChoiceStatement.label+"(msg.cont, External)\n")
        } else {
          mon.append("\t\t\t\t\tsend"+sendChoiceStatement.label+"(msg.cont, External)\n")
        }

      case receiveStatement: ReceiveStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\treceive" + receiveStatement.label + "(msg.cont, External)\n")
        } else {
          mon.append("\t\t\t\t\treceive" + receiveStatement.label + "(msg.cont, External)\n")
        }

      case receiveChoiceStatement: ReceiveChoiceStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\treceive"+receiveChoiceStatement.label+"(msg.cont, External)\n")
        } else {
          mon.append("\t\t\t\t\treceive"+receiveChoiceStatement.label+"(msg.cont, External)\n")
        }

      case recursiveVar: RecursiveVar =>
        handleSendNextCase(currentStatement, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name))

      case recursiveStatement: RecursiveStatement =>
        handleSendNextCase(currentStatement, recursiveStatement.body)

      case _ =>
    }
  }

  def handleReceive(statement: ReceiveStatement, nextStatement: Statement): Unit = {
    if (first) {
      mon.append("      receive" + statement.label + "(Internal, cm)\n      cm.close()\n  }\n")
      first = false
    }

    mon.append("  def receive" + statement.label + "(internal: Out[" + statement.label + "], External: ConnectionManager): Any = {\n")
    mon.append("    External.receive() match {\n")
    mon.append("      case msg @ " + statement.label + "(")
    addParameters(statement.types)
    mon.append(")=>\n")
    if(statement.condition != null){
      handleCondition(statement.condition, statement.label)
      handleReceiveNextCase(statement, nextStatement)
      mon.append("        } else {\n")
    } else {
      handleReceiveNextCase(statement, nextStatement)
    }
    if(statement.condition != null){
      mon.append("        }\n")
    }
    mon.append("      case _ =>\n")
    mon.append("    }\n  }\n")
  }

  @scala.annotation.tailrec
  private def handleReceiveNextCase(currentStatement: ReceiveStatement, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
        handleReceiveCases(currentStatement)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tsend" + sendStatement.label + "(cont, External)\n")
        } else {
          mon.append("\t\t\t\t\tsend" + sendStatement.label + "(cont, External)\n")
        }

      case sendChoiceStatement: SendChoiceStatement =>
        handleReceiveCases(currentStatement)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tsend" + sendChoiceStatement.label + "(cont, External)\n")
        } else {
          mon.append("\t\t\t\t\tsend" + sendChoiceStatement.label + "(cont, External)\n")
        }

      case receiveStatement: ReceiveStatement =>
        handleReceiveCases(currentStatement)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\treceive" + receiveStatement.label + "(cont, External)\n")
        } else {
          mon.append("\t\t\t\t\treceive" + receiveStatement.label + "(cont, External)\n")
        }

      case receiveChoiceStatement: ReceiveChoiceStatement =>
        handleReceiveCases(currentStatement)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.label)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\treceive" + receiveChoiceStatement.label + "(cont, External)\n")
        } else {
          mon.append("\t\t\t\t\treceive" + receiveChoiceStatement.label + "(cont, External)\n")
        }

      case recursiveVar: RecursiveVar =>
        handleReceiveNextCase(currentStatement, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name))

      case recursiveStatement: RecursiveStatement =>
        handleReceiveNextCase(currentStatement, recursiveStatement.body)

      case _ =>
        if(currentStatement.condition==null) {
          mon.append("internal ! msg\n")
        } else {
          mon.append("\tinternal ! msg\n")
        }
    }
  }

  private def handleReceiveCases(statement: ReceiveStatement): Unit = {
    if(statement.condition != null) {
      mon.append("val cont = internal !! " + statement.label + "(")
    } else {
      mon.append("\t\t\t\tval cont = internal !! " + statement.label + "(")
    }
    for ((k, v) <- statement.types) {
      if ((k, v) == statement.types.last) {
        mon.append("msg." + k)
      } else {
        mon.append("msg." + k + ", ")
      }
    }
    mon.append(")_\n")
  }

  def handleSendChoice(statement: SendChoiceStatement): Unit ={
    if (first) {
      mon.append("      send" + statement.label + "(Internal, cm)\n      cm.close()\n  }\n")
      first = false
    }

    mon.append("  def send" + statement.label + "(internal: In[" + statement.label + "], External: ConnectionManager): Any = {\n")
    mon.append("    internal ? {\n")

    for (choice <- statement.choices){
      mon.append("      case msg @ "+choice.asInstanceOf[SendStatement].label+"(")
      addParameters(choice.asInstanceOf[SendStatement].types)
      mon.append(") =>\n")
      if(choice.asInstanceOf[SendStatement].condition != null){
        handleCondition(choice.asInstanceOf[SendStatement].condition, choice.asInstanceOf[SendStatement].label)
        mon.append("External.send(msg)\n")
        handleSendNextCase(choice.asInstanceOf[SendStatement], choice.asInstanceOf[SendStatement].continuation)
        mon.append("        } else {\n")
      } else {
        mon.append("        External.send(msg)\n")
        handleSendNextCase(choice.asInstanceOf[SendStatement], choice.asInstanceOf[SendStatement].continuation)
      }
      if(choice.asInstanceOf[SendStatement].condition != null) {
        mon.append("        }\n")
      }
    }
    mon.append("    }\n  }\n")
  }

  def handleReceiveChoice(statement: ReceiveChoiceStatement): Unit = {
    if (first) {
      mon.append("      receive" + statement.label + "(Internal, cm)\n      cm.close()\n  }\n")
      first = false
    }

    mon.append("  def receive" + statement.label + "(internal: Out[" + statement.label + "], External: ConnectionManager): Any = {\n")
    mon.append("    External.receive() match {\n")

    for (choice <- statement.choices){
      mon.append("      case msg @ " + choice.asInstanceOf[ReceiveStatement].label + "(")
      addParameters(choice.asInstanceOf[ReceiveStatement].types)
      mon.append(")=>\n")
      if(choice.asInstanceOf[ReceiveStatement].condition != null){
        handleCondition(choice.asInstanceOf[ReceiveStatement].condition, choice.asInstanceOf[ReceiveStatement].label)
        handleReceiveNextCase(choice.asInstanceOf[ReceiveStatement], choice.asInstanceOf[ReceiveStatement].continuation)
        mon.append("        } else {\n")
      } else {
        mon.append("        ")
        handleReceiveNextCase(choice.asInstanceOf[ReceiveStatement], choice.asInstanceOf[ReceiveStatement].continuation)
      }
      if(choice.asInstanceOf[ReceiveStatement].condition != null) {
        mon.append("        }\n")
      }
    }
    mon.append("      case _ =>\n")
    mon.append("    }\n  }\n")
  }

  private def addParameters(types: Map[String, String]): Unit ={
    for (typ <- types) {
      if(typ == types.last){
        mon.append("_")
      } else {
        mon.append("_, ")
      }
    }
  }

  private def handleCondition(condition: String, label: String): Unit ={
    mon.append("        if(")
    var stringCondition = condition
    val identifierNames = sessionTypeInterpreter.getIdentifiers(condition)
    for(identName <- identifierNames){
      val varScope = sessionTypeInterpreter.searchIdent(label, identName)
      val identPattern = ("\\b"+identName+"\\b").r
      if(label == varScope){
        stringCondition = identPattern.replaceAllIn(stringCondition, "msg."+identName)
      } else {
        stringCondition = identPattern.replaceAllIn(stringCondition, "payloads."+varScope+"."+identName)
      }
    }
    mon.append(stringCondition+"){\n          ")
  }

  def end(): Unit = {
    mon.append("}")
  }
}
