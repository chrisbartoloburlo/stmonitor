package monitor.synth

import monitor.interpreter.STInterpreter
import monitor.model._

class SynthMon(sessionTypeInterpreter: STInterpreter, path: String) {
  private val mon = new StringBuilder()

  def getMon(): StringBuilder = {
    mon
  }

  private var first = true

  def startInit(statement: Statement): Unit = {
    mon.append("import lchannels.{In, Out}\nimport monitor.util.ConnectionManager\nimport scala.concurrent.ExecutionContext\nimport scala.concurrent.duration.Duration\nimport scala.util.control.TailCalls.{TailRec, done, tailcall}\n")
    mon.append("class Mon(external: ConnectionManager, internal: $, max: Int, zvalue: Double)")
    mon.append("(implicit ec: ExecutionContext, timeout: Duration) extends Runnable {\n")
    mon.append("\tobject labels {\n")
  }

  /**
   * Generates the code for storing values used from other parts in the monitor.
   *
   * @param label The label of the current statement.
   * @param probability The actual probability stated in the type.
   */
  def handleLabels(label: String, probability: Double, choice: Boolean): Unit ={
    mon.append("\t\tobject "+label+" {\n")
    mon.append("\t\t\tvar counter = 0\n")
    if(!choice){
      mon.append("\t\t\tval prob = "+probability+"\n")
      mon.append("\t\t\tvar alert = false\n")
    }
    mon.append("\t\t}\n")
  }

  def endInit(): Unit = {
    mon.append("\t}\n")
    mon.append("\toverride def run(): Unit = {\n    println(\"[Mon] Monitor started\")\n    println(\"[Mon] Setting up connection manager\")\n")
    mon.append("\t\texternal.setup()\n")
  }

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
    mon.append("\t\t\t\texternal.send(msg)\n")
    handleSendNextCase(statement, isUnique, nextStatement)
    mon.append("\t\t}\n\t}\n")
  }

  @scala.annotation.tailrec
  private def handleSendNextCase(currentStatement: SendStatement, isUnique: Boolean, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend"+sendStatement.statementID+"(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendStatement.statementID+"(msg.cont, external, 0)) }\n")

      case sendChoiceStatement: SendChoiceStatement =>
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend"+sendChoiceStatement.label+"(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendChoiceStatement.label+"(msg.cont, external, 0)) }\n")

      case receiveStatement: ReceiveStatement =>
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive" + receiveStatement.statementID + "(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(receive" + receiveStatement.statementID + "(msg.cont, external, 0)) }\n")

      case receiveChoiceStatement: ReceiveChoiceStatement =>
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive"+receiveChoiceStatement.label+"(msg.cont, external, count+1)\n\t\t\t\t} else { tailcall(receive"+receiveChoiceStatement.label+"(msg.cont, external, 0)) }\n")

      case recursiveVar: RecursiveVar =>
        handleSendNextCase(currentStatement, isUnique, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name))

      case recursiveStatement: RecursiveStatement =>
        handleSendNextCase(currentStatement, isUnique, recursiveStatement.body)

      case _ =>
          mon.append("\t\t\t\t\tdone()\n")
    }
  }

  def handleReceive(statement: ReceiveStatement, nextStatement: Statement, isUnique: Boolean): Unit = {
    var reference = statement.label
    if(!isUnique){
      reference = statement.statementID
    }
    if(first) {
      mon.replace(mon.indexOf("$"), mon.indexOf("$")+1, "Out["+reference+"]")
      mon.append("\t\treceive" + statement.statementID + "(internal, external, 0).result\n    external.close()\n  }\n")
//      reference = statement.statementID
      first = false
    }

    mon.append("  def receive" + statement.statementID + "(internal: Out[" + reference + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\texternal.receive() match {\n")
    mon.append("\t\t\tcase msg @ " + reference + "(")
    addParameters(statement.types)
    mon.append(")=>\n")
    handleReceiveNextCase(statement, isUnique, nextStatement)
    mon.append("\t\t\tcase _ => done()\n")
    mon.append("\t\t}\n\t}\n")
  }

  @scala.annotation.tailrec
  private def handleReceiveNextCase(currentStatement: ReceiveStatement, isUnique: Boolean, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
        handleReceiveCases(currentStatement, isUnique)
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend" + sendStatement.statementID + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendStatement.statementID+"(cont, external, 0)) }\n")

      case sendChoiceStatement: SendChoiceStatement =>
        handleReceiveCases(currentStatement, isUnique)
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\tsend" + sendChoiceStatement.label + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(send"+sendChoiceStatement.label+"(cont, external,0)) }\n")

      case receiveStatement: ReceiveStatement =>
        handleReceiveCases(currentStatement, isUnique)
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive" + receiveStatement.statementID + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(receive"+ receiveStatement.statementID +"(cont, external,0)) }\n")

      case receiveChoiceStatement: ReceiveChoiceStatement =>
        handleReceiveCases(currentStatement, isUnique)
        mon.append("\t\t\t\tif (count < max) {\n\t\t\t\t\treceive" + receiveChoiceStatement.label + "(cont, external, count+1)\n\t\t\t\t} else { tailcall(receive"+ receiveChoiceStatement.label +"(cont, external,0)) }\n")

      case recursiveVar: RecursiveVar =>
        handleReceiveNextCase(currentStatement, isUnique, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name))

      case recursiveStatement: RecursiveStatement =>
        handleReceiveNextCase(currentStatement, isUnique, recursiveStatement.body)

      case _ =>
        mon.append("\tinternal ! msg; done()\n")
    }
  }

  private def handleReceiveCases(statement: ReceiveStatement, isUnique: Boolean): Unit = {
    var reference = statement.statementID
    if(isUnique){
      reference = statement.label
    }
    mon.append("val cont = internal !! " + reference + "(")
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
    if(first) {
      mon.replace(mon.indexOf("$"), mon.indexOf("$")+1, "In["+statement.label+"]")
      mon.append("\t\tsend" + statement.label + "(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    mon.append("\tdef send" + statement.label + "(internal: In[" + statement.label + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\tlabels."+statement.label+".counter+=1\n")
    mon.append("\t\tinternal ? {\n")

    for (choice <- statement.choices){
      var reference = choice.asInstanceOf[SendStatement].label
      if(!sessionTypeInterpreter.getScope(choice).isUnique){
        reference = choice.asInstanceOf[SendStatement].statementID
      }
      mon.append("\t\t\tcase msg @ "+reference+"(")
      addParameters(choice.asInstanceOf[SendStatement].types)
      mon.append(") =>\n")
      mon.append("\t\t\t\tlabels."+choice.asInstanceOf[SendStatement].statementID+".counter+=1\n")
      mon.append("\t\t\t\tcheck"+statement.label+"Intervals()\n\t\t\t\t")
      mon.append("\t\t\t\texternal.send(msg)\n")
      handleSendNextCase(choice.asInstanceOf[SendStatement], isUnique = true, choice.asInstanceOf[SendStatement].continuation)
    }
    mon.append("\t\t}\n\t}\n")
  }

  def handleReceiveChoice(statement: ReceiveChoiceStatement): Unit = {
    if(first) {
      mon.replace(mon.indexOf("$"), mon.indexOf("$")+1, "Out["+statement.label+"]")
      mon.append("\t\treceive" + statement.label + "(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    mon.append("\tdef receive" + statement.label + "(internal: Out[" + statement.label + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\tlabels."+statement.label+".counter+=1\n")
    mon.append("\t\texternal.receive() match {\n")

    for (choice <- statement.choices){
      var reference = choice.asInstanceOf[ReceiveStatement].label
      if(!sessionTypeInterpreter.getScope(choice).isUnique){
        reference = choice.asInstanceOf[ReceiveStatement].statementID
      }
      mon.append("\t\t\tcase msg @ " + reference + "(")
      addParameters(choice.asInstanceOf[ReceiveStatement].types)
      mon.append(")=>\n")
      mon.append("\t\t\t\tlabels."+choice.asInstanceOf[ReceiveStatement].statementID+".counter+=1\n")
      mon.append("\t\t\t\tcheck"+statement.label+"Intervals()\n\t\t\t\t")
      handleReceiveNextCase(choice.asInstanceOf[ReceiveStatement], isUnique = true, choice.asInstanceOf[ReceiveStatement].continuation)
    }
    mon.append("\t\t\tcase _ => done()\n")
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

  def end(): Unit = {
    mon.append("}")
  }
}
