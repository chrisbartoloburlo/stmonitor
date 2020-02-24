package monitor.synth

import java.io.{File, PrintWriter}

import monitor.interpreter.STInterpreter
import monitor.model._

class SynthMon(sessionTypeInterpreter: STInterpreter, path: String) {
  private val mon = new PrintWriter(new File(path+"/Mon.scala"))

  var first = true

  def init(statement: Statement): Unit = {
    mon.write("import akka.actor._\nimport lchannels.{In, Out}\nimport scala.concurrent.ExecutionContext\nimport scala.concurrent.duration.Duration\nimport scala.collection.mutable\nclass Mon(Internal: ")

    statement match {
      case ReceiveStatement(label, _, _, _) =>
        mon.write("Out["+label+"])")
      case SendStatement(label, _, _, _) =>
        mon.write("In["+label+"])")
      case ReceiveChoiceStatement(label, _) =>
        mon.write("Out["+label+"])")
      case SendChoiceStatement(label, _) =>
        mon.write("In["+label+"])")
    }

    mon.write("(implicit ec: ExecutionContext, timeout: Duration) extends Actor {\n")
    mon.write("  private var value = new mutable.HashMap[(String, String), Any]\n")
    mon.write("  def receive: Receive = {\n    case start =>\n      println(\"[Mon] Monitor started\")\n      println(\"[Mon] Setting up connection manager\")\n")
    mon.write("      val cm = new ConnectionManager()\n      cm.setup()\n")
  }

  def handleSend(statement: SendStatement, nextStatement: Statement): Unit = {
    if(first){
      mon.write("      send"+statement.label+"(Internal, cm)\n  }\n")
      first = false
    }

    try {
      mon.write("  def send"+statement.label+"(internal: In["+sessionTypeInterpreter.getBranchLabel(statement)+"], External: ConnectionManager): Any = {\n")
    } catch {
      case _: Throwable =>
        mon.write("  def send"+statement.label+"(internal: In["+statement.label+"], External: ConnectionManager): Any = {\n")
    }

//    mon.write("  def send"+statement.label+"(internal: In["+statement.label+"], External: ConnectionManager): Any = {\n")
    mon.write("    internal ? {\n")
    mon.write("      case msg @ "+statement.label+"(")
    addParameters(statement.types)
    mon.write(") =>\n")

    if(statement.condition != null){
//      mon.write("        if("+statement.condition+"){\n")
      handleCondition(statement.condition, statement.label)
//      handleAssertion(statement.condition)
      if(statement.branch != null){
        handleSendNextCase(statement, statement.branch.left.head)
        mon.write("        } else {\n          ")
        handleSendNextCase(statement, statement.branch.right.head)
      } else {
        mon.write("External.send(msg)\n")
        handleSendNextCase(statement, nextStatement)
        mon.write("        } else {\n")
      }
    } else {
      mon.write("        External.send(msg)\n")
      handleSendNextCase(statement, nextStatement)
    }
    if(statement.condition != null){
      mon.write("        }\n")
    }
    mon.write("    }\n  }\n")
  }

  private def storeValue(types: Map[String, String], checkCondition: Boolean): Unit = {
    for((name, _) <- types) {
      val (varScope, (global, _)) = sessionTypeInterpreter.getVarInfo(name)
      if(global) {
        if(checkCondition){
          mon.write("\t\t\t\t\t\tvalue((\""+varScope+"\", \""+name+"\")) = msg."+name+"\n")
        } else {
          mon.write("\t\t\t\t\tvalue((\""+varScope+"\", \""+name+"\")) = msg."+name+"\n")
        }
      }
    }
  }

  @scala.annotation.tailrec
  private def handleSendNextCase(currentStatement: SendStatement, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\tsend"+sendStatement.label+"(msg.cont, External)\n")
        } else {
          mon.write("\t\t\t\t\tsend"+sendStatement.label+"(msg.cont, External)\n")
        }

      case sendChoiceStatement: SendChoiceStatement =>
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\tsend"+sendChoiceStatement.label+"(msg.cont, External)\n")
        } else {
          mon.write("\t\t\t\t\tsend"+sendChoiceStatement.label+"(msg.cont, External)\n")
        }

      case receiveStatement: ReceiveStatement =>
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\treceive" + receiveStatement.label + "(msg.cont, External)\n")
        } else {
          mon.write("\t\t\t\t\treceive" + receiveStatement.label + "(msg.cont, External)\n")
        }

      case receiveChoiceStatement: ReceiveChoiceStatement =>
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\treceive"+receiveChoiceStatement.label+"(msg.cont, External)\n")
        } else {
          mon.write("\t\t\t\t\treceive"+receiveChoiceStatement.label+"(msg.cont, External)\n")
        }

      case recursiveVar: RecursiveVar =>
        handleSendNextCase(currentStatement, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name).head)
//        handleSendNextCase(currentStatement, sessionTypeInterpreter.getCurrentScope.recVariables(recursiveVar.name).head)

      case recursiveStatement: RecursiveStatement =>
        handleSendNextCase(currentStatement, recursiveStatement.body.head)

      case _ =>
    }
  }

  def handleReceive(statement: ReceiveStatement, nextStatement: Statement): Unit = {
    if (first) {
      mon.write("      receive" + statement.label + "(Internal, cm)\n  }\n")
      first = false
    }

    mon.write("  def receive" + statement.label + "(internal: Out[" + statement.label + "], External: ConnectionManager): Any = {\n")
    mon.write("    External.receive() match {\n")
    mon.write("      case msg @ " + statement.label + "(")
    addParameters(statement.types)
    mon.write(")=>\n")
    if(statement.condition != null){
//      mon.write("        if("+statement.condition+"){\n")
      handleCondition(statement.condition, statement.label)
//      handleAssertion(statement.condition)
      if(statement.branch != null){
        handleReceiveNextCase(statement, statement.branch.left.head)
        mon.write("        } else {\n          ")
        handleReceiveNextCase(statement, statement.branch.right.head)
      } else {
        handleReceiveNextCase(statement, nextStatement)
        mon.write("        } else {\n")
      }
    } else {
      handleReceiveNextCase(statement, nextStatement)
    }
    if(statement.condition != null){
      mon.write("        }\n")
    }

    mon.write("    }\n  }\n")
  }

  @scala.annotation.tailrec
  private def handleReceiveNextCase(currentStatement: ReceiveStatement, nextStatement: Statement): Unit ={
    nextStatement match {
      case sendStatement: SendStatement =>
        handleReceiveCases(currentStatement)
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\tsend" + sendStatement.label + "(cont, External)\n")
        } else {
          mon.write("\t\t\t\t\tsend" + sendStatement.label + "(cont, External)\n")
        }

      case sendChoiceStatement: SendChoiceStatement =>
        handleReceiveCases(currentStatement)
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\tsend" + sendChoiceStatement.label + "(cont, External)\n")
        } else {
          mon.write("\t\t\t\t\tsend" + sendChoiceStatement.label + "(cont, External)\n")
        }

      case receiveStatement: ReceiveStatement =>
        handleReceiveCases(currentStatement)
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\treceive" + receiveStatement.label + "(cont, External)\n")
        } else {
          mon.write("\t\t\t\t\treceive" + receiveStatement.label + "(cont, External)\n")
        }

      case receiveChoiceStatement: ReceiveChoiceStatement =>
        handleReceiveCases(currentStatement)
//        appendToGlobalVar(currentStatement.label)
        storeValue(currentStatement.types, currentStatement.condition==null)
        if(currentStatement.condition==null) {
          mon.write("\t\t\t\treceive" + receiveChoiceStatement.label + "(cont, External)\n")
        } else {
          mon.write("\t\t\t\t\treceive" + receiveChoiceStatement.label + "(cont, External)\n")
        }

      case recursiveVar: RecursiveVar =>
        handleReceiveNextCase(currentStatement, sessionTypeInterpreter.getRecursiveVarScope(recursiveVar).recVariables(recursiveVar.name).head)
      //        handleReceiveNextCase(currentStatement, sessionTypeInterpreter.getCurrentScope.recVariables(recursiveVar.name).head)

      case recursiveStatement: RecursiveStatement =>
        handleReceiveNextCase(currentStatement, recursiveStatement.body.head)

      case _ =>
        mon.write("\t\t\t\tinternal ! msg\n")
    }
  }

  private def handleReceiveCases(statement: ReceiveStatement): Unit = {
    if(statement.condition != null) {
      mon.write("val cont = internal !! " + statement.label + "(")
    } else {
      mon.write("\t\t\t\tval cont = internal !! " + statement.label + "(")
    }
    for ((k, v) <- statement.types) {
      if ((k, v) == statement.types.last) {
        mon.write("msg." + k)
      } else {
        mon.write("msg." + k + ", ")
      }
    }
    mon.write(")_\n")
  }

  def handleSendChoice(statement: SendChoiceStatement, nextStatement: Statement): Unit ={
    if (first) {
      mon.write("      send" + statement.label + "(Internal, cm)\n  }\n")
      first = false
    }

    mon.write("  def send" + statement.label + "(internal: In[" + statement.label + "], External: ConnectionManager): Any = {\n")
    mon.write("    internal ? {\n")

    for (choice <- statement.choices){
      mon.write("      case msg @ "+choice.head.asInstanceOf[SendStatement].label+"(")
      addParameters(choice.head.asInstanceOf[SendStatement].types)
      mon.write(") =>\n")
      if(choice.head.asInstanceOf[SendStatement].condition != null){
//        mon.write("        if("+choice.head.asInstanceOf[SendStatement].condition+"){\n")
        handleCondition(choice.head.asInstanceOf[SendStatement].condition, choice.head.asInstanceOf[SendStatement].label)
//        handleAssertion(choice.head.asInstanceOf[SendStatement].condition)
        if(choice.head.asInstanceOf[SendStatement].branch != null){
          handleSendNextCase(choice.head.asInstanceOf[SendStatement], choice.head.asInstanceOf[SendStatement].branch.left.head)
          mon.write("        } else {\n          ")
          handleSendNextCase(choice.head.asInstanceOf[SendStatement], choice.head.asInstanceOf[SendStatement].branch.right.head)
        } else {
          mon.write("External.send(msg)\n")
          handleSendNextCase(choice.head.asInstanceOf[SendStatement], if(choice.tail.nonEmpty) choice.tail.head else new EndOfSessionType)
          mon.write("        } else {\n")
        }
      } else {
        mon.write("        External.send(msg)\n")
        handleSendNextCase(choice.head.asInstanceOf[SendStatement], if(choice.tail.nonEmpty) choice.tail.head else new EndOfSessionType)
      }
      if(choice.head.asInstanceOf[SendStatement].condition != null) {
        mon.write("        }\n")
      }
    }
    mon.write("    }\n  }\n")
  }

  def handleReceiveChoice(statement: ReceiveChoiceStatement, nextStatement: Statement): Unit = {
    if (first) {
      mon.write("      receive" + statement.label + "(Internal, cm)\n  }\n")
      first = false
    }

    mon.write("  def receive" + statement.label + "(internal: Out[" + statement.label + "], External: ConnectionManager): Any = {\n")
    mon.write("    External.receive() match {\n")

    for (choice <- statement.choices){
      mon.write("      case msg @ " + choice.head.asInstanceOf[ReceiveStatement].label + "(")
      addParameters(choice.head.asInstanceOf[ReceiveStatement].types)
      mon.write(")=>\n")
      if(choice.head.asInstanceOf[ReceiveStatement].condition != null){
//        mon.write("        if("+choice.head.asInstanceOf[ReceiveStatement].condition+"){\n")
        handleCondition(choice.head.asInstanceOf[ReceiveStatement].condition, choice.head.asInstanceOf[ReceiveStatement].label)
//        handleAssertion(choice.head.asInstanceOf[ReceiveStatement].condition)
        if(choice.head.asInstanceOf[ReceiveStatement].branch != null){
          handleReceiveNextCase(choice.head.asInstanceOf[ReceiveStatement], choice.head.asInstanceOf[ReceiveStatement].branch.left.head)
          mon.write("        } else {\n          ")
          handleReceiveNextCase(choice.head.asInstanceOf[ReceiveStatement], choice.head.asInstanceOf[ReceiveStatement].branch.right.head)
        } else {
          handleReceiveNextCase(choice.head.asInstanceOf[ReceiveStatement], if(choice.tail.nonEmpty) choice.tail.head else new EndOfSessionType)
          mon.write("        } else {\n")
        }
      } else {
        mon.write("        ")
        handleReceiveNextCase(choice.head.asInstanceOf[ReceiveStatement], if(choice.tail.nonEmpty) choice.tail.head else new EndOfSessionType)
      }
      if(choice.head.asInstanceOf[ReceiveStatement].condition != null) {
        mon.write("        }\n")
      }
    }
    mon.write("    }\n  }\n")
  }

  private def addParameters(types: Map[String, String]): Unit ={
    for (typ <- types) {
      if(typ == types.last){
        mon.write("_")
      } else {
        mon.write("_, ")
      }
    }
  }

  private def handleCondition(condition: String, label: String): Unit ={
    mon.write("        if(")
    var stringCondition = condition
    val identifierNames = sessionTypeInterpreter.getIdentifiers(condition)
    for(identName <- identifierNames){
      val varScope = sessionTypeInterpreter.searchIdent(label, identName)
      val (_, typ) = sessionTypeInterpreter.getScope(varScope).variables(identName)
      val identPattern = ("\\b"+identName+"\\b").r
      if(label == varScope){
        stringCondition = identPattern.replaceAllIn(stringCondition, "msg."+identName)
      } else {
        stringCondition = identPattern.replaceAllIn(stringCondition, "value(\""+varScope+"\", \""+identName+"\").asInstanceOf["+typ+"]")
      }
    }
    mon.write(stringCondition+"){\n          ")
  }

  def end(): Unit ={
    mon.write("}")
    mon.close()
  }
}
