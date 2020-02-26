package monitor.interpreter

import com.typesafe.scalalogging.Logger
import monitor.model._
import monitor.model.Scope
import monitor.synth.{SynthMon, SynthProtocol}

import scala.collection.mutable
import scala.reflect.runtime._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class STInterpreter(sessionType: SessionType, path: String) {
  private val toolbox = currentMirror.mkToolBox()

  private var scopes = new mutable.HashMap[String, Scope]()
  private var curScope = "global"
  scopes(curScope) = new Scope("global", null)

  private var branches = new mutable.HashMap[Statement, String]()

  var synthMon = new SynthMon(this, path)
  var synthProtocol = new SynthProtocol(this, path)

  val logger: Logger = Logger("STInterpreter")

  def getRecursiveVarScope(recursiveVar: RecursiveVar): Scope = {
    checkRecVariable(scopes(curScope), recursiveVar)
  }

  def getBranchLabel(statement: Statement): String = {
    branches(statement)
  }

  def getCurScope(): String ={
    curScope
  }

  def getScope(scopeName: String): Scope = {
    scopes(scopeName)
  }

  def run() {
    sessionType.statement match {
      case recursiveStatement: RecursiveStatement =>
        var tmpStatement: Statement = null
        while(tmpStatement.isInstanceOf[RecursiveStatement]){
          tmpStatement = recursiveStatement.body
        }
        synthMon.startInit(recursiveStatement.body)
      case _ =>
        synthMon.startInit(sessionType.statement)
    }
    synthProtocol.init()

    initialWalk(sessionType.statement)
    curScope = "global"
    synthMon.endInit()

    walk(sessionType.statement)
    synthMon.end()
    synthProtocol.end()
  }

  def initialWalk(root: Statement): Unit = {
    root match {
      case s @ ReceiveStatement(label, types, condition, continuation) =>
        createAndUpdateScope(label)
        checkAndInitVariables(label, types, condition)
        synthMon.handlePayloads(label, types)
        initialWalk(continuation)

      case SendStatement(label, types, condition, continuation) =>
        createAndUpdateScope(label)
        checkAndInitVariables(label, types, condition)
        synthMon.handlePayloads(label, types)
        initialWalk(continuation)

      case ReceiveChoiceStatement(label, choices) =>
        createAndUpdateScope(label)
        for(choice <- choices) {
          createAndUpdateScope(choice.asInstanceOf[ReceiveStatement].label)
          checkAndInitVariables(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].types, choice.asInstanceOf[ReceiveStatement].condition)
          synthMon.handlePayloads(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].types)
          initialWalk(choice.asInstanceOf[ReceiveStatement].continuation)
          curScope = scopes(choice.asInstanceOf[ReceiveStatement].label).parentScope.name
        }

      case SendChoiceStatement(label, choices) =>
        createAndUpdateScope(label)
        for(choice <- choices) {
          createAndUpdateScope(choice.asInstanceOf[SendStatement].label)
          checkAndInitVariables(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].types, choice.asInstanceOf[SendStatement].condition)
          synthMon.handlePayloads(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].types)
          initialWalk(choice.asInstanceOf[SendStatement].continuation)
          curScope = scopes(choice.asInstanceOf[SendStatement].label).parentScope.name
        }

      case RecursiveStatement(label, body) =>
        scopes(curScope).recVariables(label) = body
        initialWalk(body)

      case RecursiveVar(name, continuation) =>
        initialWalk(continuation)

      case End() =>
    }
  }

  def walk(statement: Statement): Unit = {
    statement match {
      case statement @ ReceiveStatement(label, types, condition, _) =>
        logger.info("Receive "+label+"("+types+")")
        curScope = label
        checkCondition(label, types, condition)

        synthMon.handleReceive(statement, statement.continuation)
        synthProtocol.handleReceive(statement, statement.continuation, null)
        walk(statement.continuation)

      case statement @ SendStatement(label, types, condition, _) =>
        logger.info("Send "+label+"("+types+")")
        curScope = label
        checkCondition(label, types, condition)

        synthMon.handleSend(statement, statement.continuation)
        synthProtocol.handleSend(statement, statement.continuation, null)
        walk(statement.continuation)

      case statement @ ReceiveChoiceStatement(label, choices) =>
        logger.info("Receive Choice Statement "+label+"{"+choices+"}")
        curScope = label
        synthMon.handleReceiveChoice(statement)
        synthProtocol.handleReceiveChoice(statement.label)

        for(choice <- choices) {
          curScope = choice.asInstanceOf[ReceiveStatement].label
          checkCondition(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].types, choice.asInstanceOf[ReceiveStatement].condition)
          synthProtocol.handleReceive(choice.asInstanceOf[ReceiveStatement], choice.asInstanceOf[ReceiveStatement].continuation, statement.label)

          walk(choice.asInstanceOf[ReceiveStatement].continuation)
          curScope = scopes(choice.asInstanceOf[ReceiveStatement].label).parentScope.name
        }

      //          for(choice <- statement.choices){
      //            walk(choice.tail)
      //          }

      case statement @ SendChoiceStatement(label, choices) =>
        logger.info("Send Choice Statement "+label+"{"+choices+"}")
        curScope = label
        synthMon.handleSendChoice(statement)
        synthProtocol.handleSendChoice(statement.label)

        for(choice <- choices) {
          curScope = choice.asInstanceOf[SendStatement].label
          checkCondition(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].types, choice.asInstanceOf[SendStatement].condition)

          synthProtocol.handleSend(choice.asInstanceOf[SendStatement], choice.asInstanceOf[SendStatement].continuation, statement.label)
          walk(choice.asInstanceOf[SendStatement].continuation)
          curScope = scopes(choice.asInstanceOf[SendStatement].label).parentScope.name
        }

      //          for(choice <- statement.choices){
      //            walk(choice.tail)
      //          }

      case statement @ RecursiveStatement(label, body) =>
        logger.info("Recursive statement with variable "+label+" and body: " +body)
        walk(statement.body)

      case statement @ RecursiveVar(name, continuation) =>
        logger.info("Recursive variable "+name)
        checkRecVariable(scopes(curScope), statement)
        walk(statement.continuation)

      case End() =>

      }
  }

  private def createAndUpdateScope(label: String): Unit ={
    scopes(label) = new Scope(label, scopes(curScope))
    curScope = label
  }

  @scala.annotation.tailrec
  private def checkRecVariable(scope: Scope, recursiveVar: RecursiveVar): Scope = {
    if(scope != null){
      if(!scopes(scope.name).recVariables.contains(recursiveVar.name)){
        checkRecVariable(scopes(scope.name).parentScope, recursiveVar)
      } else {
        scope
      }
    } else {
      throw new Exception("Error: Recursive variable "+recursiveVar.name+" not defined.")
    }
  }

  private def checkAndInitVariables(label: String, types: Map[String, String], condition: String): Unit ={
    for(typ <- types) {
      scopes(curScope).variables(typ._1) = (false, typ._2)
    }
    if (condition != null){
      val identifiersInCondition = getIdentifiers(condition)
      for(ident <- identifiersInCondition){
        val identScope = searchIdent(curScope, ident)
        if(identScope != curScope) {
          scopes(identScope).variables(ident) = (true, scopes(identScope).variables(ident)._2)
        }
      }
    }
  }

  def searchIdent(tmpCurScope: String, identifierName: String): String = {
    if(!scopes(tmpCurScope).variables.contains(identifierName)){
      if(scopes(tmpCurScope).parentScope==null){
        throw new Exception("STInterpreter - Identifier "+identifierName+" not in scope")
      }
      searchIdent(scopes(tmpCurScope).parentScope.name, identifierName)
    } else {
      tmpCurScope
    }
  }

  class traverser extends Traverser {
    var identifiers: List[String] = List[String]()
    override def traverse(tree: Tree): Unit = tree match {
      case i @ Ident(_) =>
        identifiers = i.name.decodedName.toString :: identifiers
        super.traverse(tree)
      case _ =>
        super.traverse(tree)
    }
  }

  def getIdentifiers(condition: String): List[String] = {
    val conditionTree = toolbox.parse(condition)
    val traverser = new traverser
    traverser.traverse(conditionTree)
    traverser.identifiers.distinct.filter(_ != "util")
  }

  private def checkCondition(label: String, types: Map[String, String], condition: String): Boolean ={
    if(condition != null) {
      var stringVariables = ""
      val identifiersInCondition = getIdentifiers(condition)
      val source = scala.io.Source.fromFile(path+"/util.scala", "utf-8")
      val util = try source.mkString finally source.close()
      for(identName <- identifiersInCondition){
        val identifier = scopes(searchIdent(curScope, identName)).variables(identName)
        stringVariables = stringVariables+"val "+identName+": "+identifier._2+"= ???;"
      }
      val eval = s"""
           |$util
           |$stringVariables
           |$condition
           |""".stripMargin
      val tree = toolbox.parse(eval)
      val checked = toolbox.typecheck(tree)
      checked.tpe == Boolean
    }
    true
  }

  def getVarInfo(identName: String): (String, (Boolean, String)) = {
    (searchIdent(curScope, identName), scopes(searchIdent(curScope, identName)).variables(identName))
  }

}
