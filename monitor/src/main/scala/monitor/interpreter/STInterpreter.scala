package monitor.interpreter

import monitor.model._
import monitor.model.Scope
import monitor.model.Expr
import monitor.synth.{SynthMon, SynthProtocol}

import scala.collection.mutable
import scala.reflect.runtime._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class STInterpreter(sessionType: SessionType, globalVar: mutable.HashMap[String, String], path: String) {
  private val toolbox = currentMirror.mkToolBox()

  private var scopes = new mutable.HashMap[String, Scope]()
  private var curScope = "global"
  scopes(curScope) = new Scope("global", null)

  private var branches = new mutable.HashMap[Statement, String]()

  var synthMon = new SynthMon(this, path)
  var synthProtocol = new SynthProtocol(this, path)

  def getGlobalVar: mutable.HashMap[String, String] = {
    globalVar
  }

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
    initialWalk(sessionType.statement)
    curScope = "global"
    sessionType.statement match {
      case recursiveStatement: RecursiveStatement =>
        var tmpStatement: Statement = null
        while(tmpStatement.isInstanceOf[RecursiveStatement]){
          tmpStatement = recursiveStatement.body
        }
        synthMon.init(recursiveStatement.body)
      case _ =>
        synthMon.init(sessionType.statement)
    }
    synthProtocol.init()
//    walk(sessionType.statement)
  }

  def initialWalk(root: Statement): Unit = {
      root match {
        case ReceiveStatement(label, types, condition, continuation) =>
          createAndUpdateScope(label)
          checkAndInitVariables(label, types, condition)
          initialWalk(continuation)

        case SendStatement(label, types, condition, continuation) =>
          createAndUpdateScope(label)
          checkAndInitVariables(label, types, condition)
          initialWalk(continuation)

        case ReceiveChoiceStatement(label, choices) =>
          createAndUpdateScope(label)
          for(choice <- choices) {
            createAndUpdateScope(choice.asInstanceOf[ReceiveStatement].label)
            checkAndInitVariables(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].types, choice.asInstanceOf[ReceiveStatement].condition)
            curScope = scopes(choice.asInstanceOf[ReceiveStatement].label).parentScope.name
            initialWalk(choice.asInstanceOf[ReceiveStatement].continuation)
          }

        case SendChoiceStatement(label, choices) =>
          createAndUpdateScope(label)
          for(choice <- choices) {
            createAndUpdateScope(choice.asInstanceOf[SendStatement].label)
            checkAndInitVariables(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].types, choice.asInstanceOf[SendStatement].condition)
            curScope = scopes(choice.asInstanceOf[SendStatement].label).parentScope.name
            initialWalk(choice.asInstanceOf[SendStatement].continuation)
          }

        case RecursiveStatement(label, body) =>
          scopes("global").recVariables(label) = body
          initialWalk(body)

        case RecursiveVar(name, continuation) =>
          initialWalk(continuation)

        case EndOfSessionType() =>
      }
  }

//  def walk(tree: Statement): Unit = {
//    if(tree.nonEmpty){
//      tree.head match {
//        case statement @ ReceiveStatement(label, types, condition, _) =>
//          println("Receive "+label+"("+types+")")
////          createAndUpdateScope(label)
//          curScope = label
//          checkCondition(label, types, condition)
////          curScope = scopes(label).parentScope.name
//          synthMon.handleReceive(statement, if(tree.tail.nonEmpty) tree.tail.head else new EndOfSessionType)
//          synthProtocol.handleReceive(statement, if(tree.tail.nonEmpty) tree.tail.head else new EndOfSessionType, null)
//          if(statement.branch != null){
//            branches(statement.branch.left.head) = statement.branch.label
//            walk(statement.branch.left :+ new EndOfSessionType)
//
//            branches(statement.branch.right.head) = statement.branch.label
//            walk(statement.branch.right :+ new EndOfSessionType)
//          }
//          walk(tree.tail)
//
//        case statement @ SendStatement(label, types, condition, _) =>
//          println("Send "+label+"("+types+")")
////          createAndUpdateScope(label)
//          curScope = label
//          checkCondition(label, types, condition)
////          curScope = scopes(label).parentScope.name
//          synthMon.handleSend(statement, if(tree.tail.nonEmpty) tree.tail.head else new EndOfSessionType)
//          synthProtocol.handleSend(statement, if(tree.tail.nonEmpty) tree.tail.head else new EndOfSessionType, null)
//          if(statement.branch != null){
//            branches(statement.branch.left.head) = statement.branch.label
//            walk(statement.branch.left :+ new EndOfSessionType)
//
//            branches(statement.branch.right.head) = statement.branch.label
//            walk(statement.branch.right :+ new EndOfSessionType)
//          }
//          walk(tree.tail)
//
//        case statement @ ReceiveChoiceStatement(label, choices) =>
//          curScope = label
//          println("Receive Choice Statement "+label+"{"+choices+"}")
//          synthMon.handleReceiveChoice(statement, if(tree.tail.nonEmpty) tree.tail.head else new EndOfSessionType)
//          synthProtocol.handleReceiveChoice(statement.label)
//
//          for(choice <- choices) {
//            createAndUpdateScope(choice.head.asInstanceOf[ReceiveStatement].label)
//            curScope = choice.head.asInstanceOf[ReceiveStatement].label
//            checkCondition(choice.head.asInstanceOf[ReceiveStatement].label, choice.head.asInstanceOf[ReceiveStatement].types, choice.head.asInstanceOf[ReceiveStatement].condition)
//
//            if(choice.head.asInstanceOf[ReceiveStatement].branch != null){
//              branches(choice.head.asInstanceOf[ReceiveStatement].branch.left.head) = choice.head.asInstanceOf[ReceiveStatement].branch.label
//              walk(choice.head.asInstanceOf[ReceiveStatement].branch.left :+ new EndOfSessionType)
//
//              branches(choice.head.asInstanceOf[ReceiveStatement].branch.right.head) = choice.head.asInstanceOf[ReceiveStatement].branch.label
//              walk(choice.head.asInstanceOf[ReceiveStatement].branch.right :+ new EndOfSessionType)
//            }
//            synthProtocol.handleReceive(choice.head.asInstanceOf[ReceiveStatement], if (choice.tail.nonEmpty) choice.tail.head else new EndOfSessionType, statement.label)
//            curScope = scopes(choice.head.asInstanceOf[ReceiveStatement].label).parentScope.name
//          }
//
//          for(choice <- statement.choices){
//            if(choice.tail.nonEmpty) walk(choice.tail)
//          }
//
//          walk(tree.tail)
//
//        case statement @ SendChoiceStatement(label, choices) =>
//          println("Send Choice Statement "+label+"{"+choices+"}")
//          curScope = label
//          synthMon.handleSendChoice(statement, if(tree.tail.nonEmpty) tree.tail.head else new EndOfSessionType)
//          synthProtocol.handleSendChoice(statement.label)
//
//          for(choice <- choices) {
////            createAndUpdateScope(choice.head.asInstanceOf[SendStatement].label)
//            curScope = choice.head.asInstanceOf[SendStatement].label
//            checkCondition(choice.head.asInstanceOf[SendStatement].label, choice.head.asInstanceOf[SendStatement].types, choice.head.asInstanceOf[SendStatement].condition)
//
//            if(choice.head.asInstanceOf[SendStatement].branch != null){
//              branches(choice.head.asInstanceOf[SendStatement].branch.left.head) = choice.head.asInstanceOf[SendStatement].branch.label
//              walk(choice.head.asInstanceOf[SendStatement].branch.left :+ new EndOfSessionType)
//
//              branches(choice.head.asInstanceOf[SendStatement].branch.right.head) = choice.head.asInstanceOf[SendStatement].branch.label
//              walk(choice.head.asInstanceOf[SendStatement].branch.right :+ new EndOfSessionType)
//            }
//            synthProtocol.handleSend(choice.head.asInstanceOf[SendStatement], if(choice.tail.nonEmpty) choice.tail.head else new EndOfSessionType, statement.label)
//            curScope = scopes(choice.head.asInstanceOf[SendStatement].label).parentScope.name
//          }
//
//          for(choice <- statement.choices){
//            if(choice.tail.nonEmpty) walk(choice.tail)
//          }
//          walk(tree.tail)
//
//        case statement @ RecursiveStatement(label, body) =>
//          println("Recursive statement with variable "+label+" and body: " +body)
//          walk(statement.body)
//          walk(tree.tail)
//
//        case statement @ RecursiveVar(name) =>
//          println("Recursive variable "+name)
//          checkRecVariable(scopes(curScope), statement)
//          walk(tree.tail)
//
//        case EndOfSessionType() =>
//
//      }
//    } else {
//      synthMon.end()
//      synthProtocol.end()
//    }
//  }

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



  private def checkAndUpdateConditionVariables(left: Expr, right: Expr): Unit ={
    left match {
      case i @ Identifier(null, name) =>
        if(!scopes(curScope).variables.contains(name)){
          val variableScope = searchVar(scopes(curScope).parentScope.name, i)
          scopes(variableScope).variables(i.name) = (true, scopes(variableScope).variables(i.name)._2)
        }
        checkAndUpdateConditionVariables(right, Condition("tt", null, null))
      case Number(_) =>
        checkAndUpdateConditionVariables(right, Condition("tt", null, null))
      case c @ Condition(_, _, _) =>
        if(c.op != "tt") {
          checkAndUpdateConditionVariables(c.left, c.right)
          checkAndUpdateConditionVariables(right, Condition("tt", null, null))
        }
    }
  }

  private def searchVar(tmpCurScope: String, identifier: Identifier): String = {
    try {
      if(!scopes(tmpCurScope).variables.contains(identifier.name)){
        searchVar(scopes(tmpCurScope).parentScope.name, identifier)
      } else {
        tmpCurScope
      }
    } catch {
      case e: java.util.NoSuchElementException =>
        throw new Exception("Error: Identifier with name : "+ identifier.name +" does not exist in any of the labels.")
    }
  }

//  private def checkVariables(label: String, types: Map[String, String], condition: Condition): Unit ={
//    for(typ <- types) {
//      scopes(curScope).variables(typ._1) = (false, typ._2)
//    }
//    if(condition.op != "tt") {
//      checkConditionVariables(condition.left, condition.right)
//      if(!typeCheckAssertions(condition: Condition)){
//        throw new Exception("Error: Condition: "+ condition +" does not typecheck to a Boolean")
//      }
//    }
//  }

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

  private def checkConditionVariables(left: Expr, right: Expr): Unit ={
    left match {
      case Identifier(null, name) =>
        if(!scopes(curScope).variables.contains(name)){
          throw new Exception("Error: Variable "+name+" not declared.")
        }
        checkConditionVariables(right, Condition("tt", null, null))
      case Identifier(location, name) =>
        if(!scopes(location).variables.contains(name)){
          throw new Exception("Error: Variable "+name+" not declared in Class "+location+".")
        }
        checkConditionVariables(right, Condition("tt", null, null))
      case Number(_) =>
        checkConditionVariables(right, Condition("tt", null, null))
      case c @ Condition(_, _, _) =>
        if(c.op != "tt") {
          checkConditionVariables(c.left, c.right)
          checkConditionVariables(right, Condition("tt", null, null))
        }
    }
  }

  private def typeCheckAssertions(condition: Condition): Boolean = {
    val (stringCondition, variables) = buildStringCondition(condition, "", mutable.Set[Identifier]())
    if(variables.nonEmpty){
      var stringVariables = ""
      for (v <- variables) {
        if(v.scope == null)
//          types(v.name)
          stringVariables = stringVariables+"val "+v.name+": "+scopes(curScope).variables(v.name)._2+"= ???;"
        else
          stringVariables = stringVariables+"val "+v.name+": "+scopes(v.scope).variables(v.name)._2+"= ???;"
      }
      val eval =
        s"""
           |$stringVariables
           |$stringCondition
           |""".stripMargin
      val tree = toolbox.parse(eval)
      val checked = toolbox.typecheck(tree)
      checked.tpe == Boolean
    }
    true
  }

  private def buildStringCondition(expr: Expr, stringCondition: String, variables: mutable.Set[Identifier]): (String, mutable.Set[Identifier]) = {
    expr match {
      case c @ Condition(_, _, _) =>
        if(c.op == "tt") return (stringCondition, variables)
        c.left match {
          case i @ Identifier(null, name) =>
            buildStringCondition(c.right, stringCondition + name + c.op, variables += i)
          case i @ Identifier(_, name) =>
            buildStringCondition(c.right, stringCondition + name + c.op, variables += i)
          case Number(value) =>
            buildStringCondition(c.right, stringCondition + value.toString + c.op, variables)
          case cl @ Condition(_, _, _) =>
            buildStringCondition(c.right, buildStringCondition(cl, stringCondition, variables)._1 + c.op, variables)
        }
      case i @ Identifier(_, name) =>
        buildStringCondition(Condition("tt", null, null), stringCondition + name, variables += i)
      case Number(value) =>
        buildStringCondition(Condition("tt", null, null), stringCondition + value.toString, variables)
    }
  }
}
