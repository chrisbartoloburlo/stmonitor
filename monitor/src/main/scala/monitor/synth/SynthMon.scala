package monitor.synth

import monitor.interpreter.STInterpreter
import monitor.model._

class SynthMon(sessionTypeInterpreter: STInterpreter, path: String) {
  private val mon = new StringBuilder()

  def getMon(): StringBuilder = {
    mon
  }

  private var first = true

  /**
   * Generates the code for declaring a monitor including the imports required for the monitor to compile.
   *
   * @param preamble The contents of the preamble file.
   */
  def startInit(preamble: String): Unit = {
    if (preamble!="") mon.append(preamble+"\n")
    mon.append("import lchannels.{In, Out}\nimport monitor.util.ConnectionManager\nimport scala.concurrent.ExecutionContext\nimport scala.concurrent.duration.Duration\nimport scala.util.control.TailCalls.{TailRec, done, tailcall}\nclass Monitor(external: ConnectionManager, internal: ")

    mon.append("$, max: Int, report: String => Unit)")

    mon.append("(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {\n")
    mon.append("\tobject payloads {\n")
  }

  /**
   * Generates the code for storing values used from other parts in the monitor.
   *
   * @param label The label of the current statement.
   * @param types A mapping from identifiers to their type.
   */
  def handlePayloads(label: String, types: Map[String, String]): Unit ={
    mon.append("\t\tobject "+label+" {\n")
    for(typ <- types){
      mon.append("\t\t\tvar "+typ._1+": "+typ._2+" = _\n")
    }
    mon.append("\t\t}\n")
  }

  def endInit(): Unit = {
    mon.append("\t}\n")
    mon.append("\toverride def run(): Unit = {\n\t\treport(\"[MONITOR] Monitor started, setting up connection manager\")\n")
    mon.append("\t\texternal.setup()\n")
  }

  /**
   * Generates the code for the external choice type consisting of a single branch: ?Label(payload)[assertion].S
   *
   * @param statement The current statement.
   * @param nextStatement The next statement in the session type.
   * @param isUnique A boolean indicating whether the label of the current statement is unique.
   */
  def handleSend(statement: SendStatement, nextStatement: Statement, isUnique: Boolean): Unit = {
    var reference = statement.label
    if(!isUnique){
      reference = statement.statementID
    }
    if(first){
      mon.replace(mon.indexOf("$"), mon.indexOf("$")+1, "In["+reference+"]")
      mon.append("\t\tsend"+statement.statementID+"(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    try {
      mon.append("\tdef send"+statement.statementID+"(internal: In["+sessionTypeInterpreter.getBranchLabel(statement)+"], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    } catch {
      case _: Throwable =>
        mon.append("\tdef send"+statement.statementID+"(internal: In["+reference+"], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    }

    mon.append("\t\tinternal ? {\n")
    mon.append("\t\t\tcase msg @ "+reference+"(")
    addParameters(statement.types)
    mon.append(") =>\n")

    if(statement.condition != null){
      handleCondition(statement.condition, statement.statementID)
      mon.append("\t\t\t\t\texternal.send(msg)\n")
      handleSendNextCase(statement, nextStatement)
      mon.append("\t\t\t\t} else {\n")
      mon.append("\t\t\t\treport(\"[MONITOR] VIOLATION in Assertion: "+statement.condition+"\"); done() }\n")
    } else {
      mon.append("\t\t\t\texternal.send(msg)\n")
      handleSendNextCase(statement, nextStatement)
    }
    mon.append("\t\t\tcase msg @ _ => report(f\"[MONITOR] VIOLATION unknown message: $msg\"); done()\n")
    mon.append("\t\t}\n\t}\n")
  }

  /**
   * Generates the code for the monitor to call the method representing the next statement in the session type after an external choice type.
   *
   * @param currentStatement The current statement.
   * @param nextStatement The next statement in the session type.
   */
  @scala.annotation.tailrec
  private def handleSendNextCase(currentStatement: SendStatement, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend"+sendStatement.statementID+"(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendStatement.statementID+"(msg.cont, external, 0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\tsend"+sendStatement.statementID+"(msg.cont, external, count+1)\n\t\t\t\t\t} else { tailcall(send"+sendStatement.statementID+"(msg.cont, external,0)) }\n")
        }

      case sendChoiceStatement: SendChoiceStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend"+sendChoiceStatement.label+"(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendChoiceStatement.label+"(msg.cont, external, 0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\tsend"+sendChoiceStatement.label+"(msg.cont, external, count+1)\n\t\t\t\t\t} else { tailcall("+sendChoiceStatement.label+"(msg.cont, external, 0)) }\n")
        }

      case receiveStatement: ReceiveStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive" + receiveStatement.statementID + "(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(receive" + receiveStatement.statementID + "(msg.cont, external, 0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\treceive" + receiveStatement.statementID + "(msg.cont, external, count+1)\n\t\t\t\t\t} else { tailcall(receive" + receiveStatement.statementID + "(msg.cont, external, 0)) }\n")
        }

      case receiveChoiceStatement: ReceiveChoiceStatement =>
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive"+receiveChoiceStatement.label+"(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(receive"+receiveChoiceStatement.label+"(msg.cont, external, 0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\treceive"+receiveChoiceStatement.label+"(msg.cont, external, count+1)\n\t\t\t\t\t} else { tailcall(receive"+receiveChoiceStatement.label+"(msg.cont, external, 0)) }\n")
        }

      case recursiveVar: RecursiveVar =>
        handleSendNextCase(currentStatement, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name))

      case recursiveStatement: RecursiveStatement =>
        handleSendNextCase(currentStatement, recursiveStatement.body)

      case _ =>
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tdone()\n")
        } else {
          mon.append("\t\t\t\t\tdone()\n")
        }
    }
  }

  /**
   * Generates the code for the internal choice type consisting of a single branch: !Label(payload)[assertion].S
   *
   * @param statement The current statement.
   * @param nextStatement The next statement in the session type.
   * @param isUnique A boolean indicating whether the label of the current statement is unique.
   */
  def handleReceive(statement: ReceiveStatement, nextStatement: Statement, isUnique: Boolean): Unit = {
    var reference = statement.label
    if(!isUnique){
      reference = statement.statementID
    }
    if(first) {
      mon.replace(mon.indexOf("$"), mon.indexOf("$")+1, "Out["+reference+"]")
      mon.append("\t\treceive" + statement.statementID + "(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    mon.append("  def receive" + statement.statementID + "(internal: Out[" + reference + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\texternal.receive() match {\n")
    mon.append("\t\t\tcase msg @ " + reference + "(")
    addParameters(statement.types)
    mon.append(")=>\n")
    if(statement.condition != null){
      handleCondition(statement.condition, statement.statementID)
      handleReceiveNextCase(statement, isUnique, nextStatement)
      mon.append("\t\t\t\t} else {\n")
      mon.append("\t\t\t\treport(\"[MONITOR] VIOLATION in Assertion: "+statement.condition+"\"); done() }\n")
    } else {
      handleReceiveNextCase(statement, isUnique, nextStatement)
    }
    mon.append("\t\t\tcase msg @ _ => report(f\"[MONITOR] VIOLATION unknown message: $msg\"); done()\n")
    mon.append("\t\t}\n\t}\n")
  }

  /**
   * Generates the code for the monitor to call the method representing the next statement in the session type after an internal choice type.
   *
   * @param currentStatement The current statement.
   * @param isUnique A boolean indicating whether the label of the current statement is unique.
   * @param nextStatement The next statement in the session type.
   */
  @scala.annotation.tailrec
  private def handleReceiveNextCase(currentStatement: ReceiveStatement, isUnique: Boolean, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
        handleReceiveCases(currentStatement, isUnique)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend" + sendStatement.statementID + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendStatement.statementID+"(cont, external, 0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\tsend" + sendStatement.statementID + "(cont, external, count+1)\n\t\t\t\t\t} else { tailcall(send"+ sendStatement.statementID +"(cont, external, 0)) }\n")
        }

      case sendChoiceStatement: SendChoiceStatement =>
        handleReceiveCases(currentStatement, isUnique)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend" + sendChoiceStatement.label + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendChoiceStatement.label+"(cont, external,0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\tsend" + sendChoiceStatement.label + "(cont, external,count+1)\n\t\t\t\t\t} else { tailcall(send" + sendChoiceStatement.label + "(cont, external,0)) }\n")
        }

      case receiveStatement: ReceiveStatement =>
        handleReceiveCases(currentStatement, isUnique)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive" + receiveStatement.statementID + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(receive"+ receiveStatement.statementID +"(cont, external,0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\treceive" + receiveStatement.statementID + "(cont, external,count+1)\n\t\t\t\t\t} else { tailcall(receive" + receiveStatement.statementID + "(cont, external,0)) }\n")
        }

      case receiveChoiceStatement: ReceiveChoiceStatement =>
        handleReceiveCases(currentStatement, isUnique)
        storeValue(currentStatement.types, currentStatement.condition==null, currentStatement.statementID)
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive" + receiveChoiceStatement.label + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(receive"+ receiveChoiceStatement.label +"(cont, external,0)) }\n")
        } else {
          mon.append("\t\t\t\t\tif (count < max) {\n\t\t\t\t\t\treceive" + receiveChoiceStatement.label + "(cont, external, count+1)\n\t\t\t\t\t} else { tailcall(receive" + receiveChoiceStatement.label + "(cont, external, 0)) }\n")
        }

      case recursiveVar: RecursiveVar =>
        handleReceiveNextCase(currentStatement, isUnique, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name))

      case recursiveStatement: RecursiveStatement =>
        handleReceiveNextCase(currentStatement, isUnique, recursiveStatement.body)

      case _ =>
        if(currentStatement.condition==null) {
          mon.append("\t\t\t\tinternal ! msg; done()\n")
        } else {
          mon.append("\t\t\t\t\tinternal ! msg; done()\n")
        }
    }
  }

  /**
   * Generates the code for forwarding a message over an lchannels channel.
   *
   * @param statement The current statement.
   * @param isUnique A boolean indicating whether the label of the current statement is unique.
   */
  private def handleReceiveCases(statement: ReceiveStatement, isUnique: Boolean): Unit = {
    var reference = statement.statementID
    if(isUnique){
      reference = statement.label
    }
    if(statement.condition != null) {
      mon.append("\t\t\t\t\tval cont = internal !! " + reference + "(")
    } else {
      mon.append("\t\t\t\tval cont = internal !! " + reference + "(")
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

  /**
   * Generates the code for the external choice type: &{?Label(payload)[assertion].S, ...}
   *
   * @param statement The current statement.
   */
  def handleSendChoice(statement: SendChoiceStatement): Unit ={
    if(first) {
      mon.replace(mon.indexOf("$"), mon.indexOf("$")+1, "In["+statement.label+"]")
      mon.append("\t\tsend" + statement.label + "(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    mon.append("\tdef send" + statement.label + "(internal: In[" + statement.label + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\tinternal ? {\n")

    for (choice <- statement.choices){
      var reference = choice.asInstanceOf[SendStatement].label
      if(!sessionTypeInterpreter.getScope(choice).isUnique){
        reference = choice.asInstanceOf[SendStatement].statementID
      }
      mon.append("\t\t\tcase msg @ "+reference+"(")
      addParameters(choice.asInstanceOf[SendStatement].types)
      mon.append(") =>\n")
      if(choice.asInstanceOf[SendStatement].condition != null){
        handleCondition(choice.asInstanceOf[SendStatement].condition, choice.asInstanceOf[SendStatement].statementID)
        mon.append("\t\t\t\t\texternal.send(msg)\n")
        handleSendNextCase(choice.asInstanceOf[SendStatement], choice.asInstanceOf[SendStatement].continuation)
        mon.append("\t\t\t\t} else {\n")
        mon.append("\t\t\t\treport(\"[MONITOR] VIOLATION in Assertion: "+choice.asInstanceOf[SendStatement].condition+"\"); done() }\n")
      } else {
        mon.append("\t\t\t\texternal.send(msg)\n")
        handleSendNextCase(choice.asInstanceOf[SendStatement], choice.asInstanceOf[SendStatement].continuation)
      }
    }
    mon.append("\t\t\tcase msg @ _ => report(f\"[MONITOR] VIOLATION unknown message: $msg\"); done()\n")
    mon.append("\t\t}\n\t}\n")
  }

  /**
   * Generates the code for the internal choice type: +{!Label(payload)[assertion].S, ...}
   *
   * @param statement The current statment.
   */
  def handleReceiveChoice(statement: ReceiveChoiceStatement): Unit = {
    if(first) {
      mon.replace(mon.indexOf("$"), mon.indexOf("$")+1, "Out["+statement.label+"]")
      mon.append("\t\treceive" + statement.label + "(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    mon.append("\tdef receive" + statement.label + "(internal: Out[" + statement.label + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\texternal.receive() match {\n")

    for (choice <- statement.choices){
      var reference = choice.asInstanceOf[ReceiveStatement].label
      if(!sessionTypeInterpreter.getScope(choice).isUnique){
        reference = choice.asInstanceOf[ReceiveStatement].statementID
      }
      mon.append("\t\t\tcase msg @ " + reference + "(")
      addParameters(choice.asInstanceOf[ReceiveStatement].types)
      mon.append(")=>\n")
      if(choice.asInstanceOf[ReceiveStatement].condition != null){
        handleCondition(choice.asInstanceOf[ReceiveStatement].condition, choice.asInstanceOf[ReceiveStatement].statementID)
        handleReceiveNextCase(choice.asInstanceOf[ReceiveStatement], true, choice.asInstanceOf[ReceiveStatement].continuation)
        mon.append("\t\t\t\t} else {\n")
        mon.append("\t\t\t\treport(\"[MONITOR] VIOLATION in Assertion: "+choice.asInstanceOf[ReceiveStatement].condition+"\"); done() }\n")
      } else {
        handleReceiveNextCase(choice.asInstanceOf[ReceiveStatement], true, choice.asInstanceOf[ReceiveStatement].continuation)
      }
    }
    mon.append("\t\t\tcase msg @ _ => report(f\"[MONITOR] VIOLATION unknown message: $msg\"); done()\n")
    mon.append("\t\t}\n\t}\n")
  }

  /**
   * Generates the parameters for the statements depending on the payload size.
   *
   * @param types A mapping from identifiers to their type.
   */
  private def addParameters(types: Map[String, String]): Unit ={
    for (typ <- types) {
      if(typ == types.last){
        mon.append("_")
      } else {
        mon.append("_, ")
      }
    }
  }

  /**
   * Generates the code for storing a value in the respective identifier object within the monitor.
   *
   * @param types A mapping from identifiers to their type.
   * @param checkCondition A boolean indicating whether current statement has a condition.
   * @param curStatementScope The label of the current statement used to retrieve identifier
   *                          information from the interpreter.
   */
  private def storeValue(types: Map[String, String], checkCondition: Boolean, curStatementScope: String): Unit = {
    for((name, _) <- types) {
      val (varScope, (global, _)) = sessionTypeInterpreter.getVarInfo(name, curStatementScope)
      if(global) {
        if(checkCondition){
          mon.append("\t\t\t\tpayloads."+varScope+"."+name+" = msg."+name+"\n")
        } else {
          mon.append("\t\t\tpayloads."+varScope+"."+name+" = msg."+name+"\n")
        }
      }
    }
  }

  /**
   * Generates the code for conditions by identifying identifiers and changing them for the
   * respective variable within the monitor.
   *
   * @param condition Condition in String format.
   * @param label The label of the current statment.
   */
  private def handleCondition(condition: String, label: String): Unit ={
    mon.append("\t\t\t\tif(")
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
    mon.append(stringCondition+"){\n")
  }

  def end(): Unit = {
    mon.append("}")
  }
}
