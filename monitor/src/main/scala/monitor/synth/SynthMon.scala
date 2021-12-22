package monitor.synth

import monitor.interpreter.STInterpreter
import monitor.model._

class SynthMon(sessionTypeInterpreter: STInterpreter, path: String) {
  private val mon = new StringBuilder()

  def getMon(): StringBuilder = {
    mon
  }

  private var first = true
  private var importIn = false
  private var importOut = false

  /**
   * Generates the code for declaring a monitor including the imports required for the monitor to compile.
   *
   * @param preamble The contents of the preamble file.
   */
  def startInit(preamble: String): Unit = {
    if (preamble!="") mon.append(preamble+"\n")
    mon.append("import lchannels.$lchannelsimport\nimport monitor.util.{ConnectionManager, logger}\nimport scala.concurrent.ExecutionContext\nimport scala.concurrent.duration.Duration\nimport scala.util.control.TailCalls.{TailRec, done, tailcall}\n")
    mon.append("class Monitor(external: ConnectionManager, internal: $channel, max: Int, zvalue: Double, log: Boolean)")
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
      mon.append("\t\t\tvar warn = false\n")
      mon.append("\t\t\tlazy val l: logger = new logger(f\"${System.getProperty(\"user.dir\")}/logs/"+label+"_log.csv\")\n")
      mon.append("\t\t\tif(log){\n")
      mon.append("\t\t\t\tl.log(\"pmin pmax pe\")\n\t\t\t}\n")
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
      mon.replace(mon.indexOf("$channel"), mon.indexOf("$channel")+8, "In["+reference+"]")
      mon.append("\t\tsend"+statement.statementID+"(internal, external, 0).result\n\t\texternal.close()\n  }\n")
      first = false
    }

    try {
      mon.append("\tdef send"+statement.statementID+"(internal: In["+sessionTypeInterpreter.getBranchLabel(statement)+"], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    } catch {
      case _: Throwable =>
        mon.append("\tdef send"+statement.statementID+"(internal: In["+reference+"], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    }
    importIn = true

    mon.append("\t\tinternal ? {\n")
    mon.append("\t\t\tcase msg @ "+reference+"(")
    addParameters(statement.types)
    mon.append(") =>\n")
    mon.append("\t\t\t\texternal.send(msg)\n")
    handleSendNextCase(statement, isUnique, nextStatement)
    mon.append("\t\t\tcase _ => done()\n")
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
      mon.replace(mon.indexOf("$channel"), mon.indexOf("$channel")+8, "Out["+reference+"]")
      mon.append("\t\treceive" + statement.statementID + "(internal, external, 0).result\n    external.close()\n  }\n")
//      reference = statement.statementID
      first = false
    }

    mon.append("  def receive" + statement.statementID + "(internal: Out[" + reference + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    importOut=true
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
        mon.append("internal ! msg; done()\n")
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
      mon.replace(mon.indexOf("$channel"), mon.indexOf("$channel")+8, "In["+statement.label+"]")
      mon.append("\t\tsend" + statement.label + "(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    mon.append("\tdef send" + statement.label + "(internal: In[" + statement.label + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\tlabels."+statement.label+".counter+=1\n")
    mon.append("\t\tinternal ? {\n")

    importIn = true
    for (choice <- statement.choices){
      var reference = choice.asInstanceOf[SendStatement].label
      if(!sessionTypeInterpreter.getScope(choice).isUnique){
        reference = choice.asInstanceOf[SendStatement].statementID
      }
      mon.append("\t\t\tcase msg @ "+reference+"(")
      addParameters(choice.asInstanceOf[SendStatement].types)
      mon.append(") =>\n")
      mon.append("\t\t\t\tlabels."+choice.asInstanceOf[SendStatement].statementID+".counter+=1\n")
      mon.append("\t\t\t\tcheck"+statement.label+"Intervals()\n")
      mon.append("\t\t\t\texternal.send(msg)\n")
      handleSendNextCase(choice.asInstanceOf[SendStatement], isUnique = true, choice.asInstanceOf[SendStatement].continuation)
    }
    mon.append("\t\t\tcase _ => done()\n")
    mon.append("\t\t}\n\t}\n")
  }

  def handleReceiveChoice(statement: ReceiveChoiceStatement): Unit = {
    if(first) {
      mon.replace(mon.indexOf("$channel"), mon.indexOf("$channel")+8, "Out["+statement.label+"]")
      mon.append("\t\treceive" + statement.label + "(internal, external, 0).result\n    external.close()\n  }\n")
      first = false
    }

    mon.append("\tdef receive" + statement.label + "(internal: Out[" + statement.label + "], external: ConnectionManager, count: Int): TailRec[Unit] = {\n")
    mon.append("\t\tlabels."+statement.label+".counter+=1\n")
    mon.append("\t\texternal.receive() match {\n")

    importOut = true
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

  def handleReceiveChoiceInterval(statement: ReceiveChoiceStatement): Unit = {
    mon.append("\tdef check"+statement.label+"Intervals(): Unit = {\n")
    for (choice <- statement.choices) {
      if(choice.asInstanceOf[ReceiveStatement].probBoundary.lessThan || choice.asInstanceOf[ReceiveStatement].probBoundary.greaterThan) {
        val choiceLabel = choice.asInstanceOf[ReceiveStatement].label
        val choiceRefId = choice.asInstanceOf[ReceiveStatement].statementID
        val pmin = "pmin_" + choiceLabel
        val pmax = "pmax_" + choiceLabel
        val pe = "pe_" + choiceLabel
        mon.append("\t\tval (" + pmin + "," + pmax + "," + pe + ") = calculateInterval(labels." + choiceRefId + ".counter, labels." + statement.label + ".counter, labels." + choiceRefId + ".prob)\n")
        mon.append("\t\tif(log){\n")
        mon.append("\t\t\tlabels."+choiceRefId+".l.log(f\"$"+pmin+" $"+pmax+" $"+pe+"\")\n")
        mon.append("\t\t}\n")
        if (choice.asInstanceOf[ReceiveStatement].probBoundary.lessThan && choice.asInstanceOf[ReceiveStatement].probBoundary.greaterThan) {
          mon.append("\t\tif(" + pmin + " >= " + pe + " || " + pmax + " <= " + pe + ") {\n")
        } else if (choice.asInstanceOf[ReceiveStatement].probBoundary.lessThan && !choice.asInstanceOf[ReceiveStatement].probBoundary.greaterThan) {
          mon.append("\t\tif(" + pmin + " >= " + pe + ") {\n")
        } else if (choice.asInstanceOf[ReceiveStatement].probBoundary.greaterThan && !choice.asInstanceOf[ReceiveStatement].probBoundary.lessThan) {
          mon.append("\t\tif(" + pmax + " <= " + pe + ") {\n")
        }
        mon.append("\t\t\tif(!labels." + choiceRefId + ".warn){\n")
        mon.append("\t\t\t\tprintln(f\"[MON] **WARN** ?" + choiceLabel + "[${" + pe + "}] outside interval [$" + pmin + ",$" + pmax + "]\")\n")
        mon.append("\t\t\t\tlabels." + choiceRefId + ".warn = true\n")
        mon.append("\t\t\t}\n")
        mon.append("\t\t} else {\n")
        mon.append("\t\t\tif(labels." + choiceRefId + ".warn){\n")
        mon.append("\t\t\t\tprintln(f\"[MON] **INFO** ?" + choiceLabel + "[${" + pe + "}] within interval [$" + pmin + ",$" + pmax + "]\")\n")
        mon.append("\t\t\t\tlabels." + choiceRefId + ".warn = false\n")
        mon.append("\t\t\t}\n")
        mon.append("\t\t}\n")
      }
    }
    mon.append("\t}\n")
  }

  def handleSendChoiceInterval(statement: SendChoiceStatement): Unit = {
    mon.append("\tdef check"+statement.label+"Intervals(): Unit = {\n")
    for (choice <- statement.choices){
      if(choice.asInstanceOf[SendStatement].probBoundary.lessThan || choice.asInstanceOf[SendStatement].probBoundary.greaterThan) {
        val choiceLabel = choice.asInstanceOf[SendStatement].label
        val choiceRefId = choice.asInstanceOf[SendStatement].statementID
        val pmin = "pmin_" + choiceLabel
        val pmax = "pmax_" + choiceLabel
        val pe = "pe_" + choiceLabel
        mon.append("\t\tval (" + pmin + "," + pmax + "," + pe + ") = calculateInterval(labels." + choiceRefId + ".counter, labels." + statement.label + ".counter, labels." + choiceRefId + ".prob)\n")
        mon.append("\t\tif(log){\n")
        mon.append("\t\t\tlabels."+choiceRefId+".l.log(f\"$"+pmin+" $"+pmax+" $"+pe+"\")\n")
        mon.append("\t\t}\n")
        if (choice.asInstanceOf[SendStatement].probBoundary.lessThan && choice.asInstanceOf[SendStatement].probBoundary.greaterThan) {
          mon.append("\t\tif(" + pmin + " >= " + pe + " || " + pmax + " <= " + pe + ") {\n")
        } else if (choice.asInstanceOf[SendStatement].probBoundary.lessThan) {
          mon.append("\t\tif(" + pmin + " >= " + pe + ") {\n")
        } else if (choice.asInstanceOf[SendStatement].probBoundary.greaterThan) {
          mon.append("\t\tif(" + pmax + " <= " + pe + ") {\n")
        }
        mon.append("\t\t\tif(!labels." + choiceRefId + ".warn){\n")
        mon.append("\t\t\t\tprintln(f\"[MON] **WARN** !" + choiceLabel + "[${" + pe + "}] outside interval [$" + pmin + ",$" + pmax + "]\")\n")
        mon.append("\t\t\t\tlabels." + choiceRefId + ".warn = true\n")
        mon.append("\t\t\t}\n")
        mon.append("\t\t} else {\n")
        mon.append("\t\t\tif(labels." + choiceRefId + ".warn){\n")
        mon.append("\t\t\t\tprintln(f\"[MON] **INFO** !" + choiceLabel + "[${" + pe + "}] within interval [$" + pmin + ",$" + pmax + "]\")\n")
        mon.append("\t\t\t\tlabels." + choiceRefId + ".warn = false\n")
        mon.append("\t\t\t}\n")
        mon.append("\t\t}\n")
      }
    }
    mon.append("\t}\n")
  }

  def addCalculateInterval(method: String):Unit = {
    mon.append("\tdef calculateInterval(count: Double, trials: Int, prob_a: Double): (Double, Double, Double) = {\n")
    if(method == "normal") {
      mon.append("\t\tval prob_e = count/trials\n")
      mon.append("\t\tval err = zvalue*math.sqrt(prob_a*(1-prob_a)/trials)\n")
      mon.append("\t\t(prob_a-err,prob_a+err,prob_e)\n")
    } else if (method == "wilson") {
      mon.append("\t\tval prob_e = (count+(0.5*(zvalue*zvalue)))/(trials+(zvalue*zvalue))\n")
      mon.append("\t\tval err = (zvalue/(trials+(zvalue*zvalue)))*math.sqrt(((count*(trials-count))/trials)+((zvalue*zvalue)/4))\n")
      mon.append("\t\t(prob_e-err,prob_e+err,prob_a)\n")
    }
    mon.append("\t}\n")
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
    if (importIn && importOut) mon.replace(mon.indexOf("$lchannelsimport"), mon.indexOf("$lchannelsimport")+16, "{In, Out}")
    else if (importIn) mon.replace(mon.indexOf("$lchannelsimport"), mon.indexOf("$lchannelsimport")+16, "In")
    else if (importOut) mon.replace(mon.indexOf("$lchannelsimport"), mon.indexOf("$lchannelsimport")+16, "Out")
    else mon.replace(mon.indexOf("$lchannelsimport"), mon.indexOf("$lchannelsimport")+16, "_")
    mon.append("}")
  }
}
