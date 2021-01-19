package monitor.interpreter

import com.typesafe.scalalogging.Logger
import monitor.model._
import monitor.model.Scope
import monitor.synth.{SynthMon, SynthProtocol}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class STInterpreter(sessionType: SessionType, path: String) {
  private val toolbox = currentMirror.mkToolBox()

  private var scopes = new mutable.HashMap[String, Scope]()
  private var scopeId = 0
  private var curScope = defineScopeName("global", scopeId)
  scopes(curScope) = new Scope("global", curScope, null)

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

  def getScopeId(): Int = {
    scopeId
  }

  def getAndUpdateScopeId(): Int = {
    val tmpId = scopeId
    scopeId+=1
    tmpId
  }

  def defineScopeName(scopeName: String, scopeId: Int): String = {
    scopeName+"_"+scopeId
  }

  /**
   * Starts the interpreter and invokes the initialisation methods of SynthMon and SynthProtocol.
   *
   * @return A tuple consisting of two string builders in which the generated code for
   *         the monitor and cpsp classes is found.
   */
  def run(): (StringBuilder, StringBuilder) = {
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
    scopeId=0
    curScope = defineScopeName("global", scopeId)
    synthMon.endInit()

    walk(sessionType.statement)
    synthMon.end()
    synthProtocol.end()
    (synthMon.getMon(), synthProtocol.getProtocol())
  }

  /**
   * Traverses the parse tree to set up the scopes found within the session type.
   *
   * @param root The root statement of the parse tree.
   */
  def initialWalk(root: Statement): Unit = {
    root match {
      case ReceiveStatement(label, types, condition, continuation) =>
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
        val tmpScope = curScope
        for(choice <- choices) {
          createAndUpdateScope(choice.asInstanceOf[ReceiveStatement].label)
          checkAndInitVariables(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].types, choice.asInstanceOf[ReceiveStatement].condition)
          synthMon.handlePayloads(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].types)
          initialWalk(choice.asInstanceOf[ReceiveStatement].continuation)
          curScope = tmpScope
        }

      case SendChoiceStatement(label, choices) =>
        createAndUpdateScope(label)
        val tmpScope = curScope
        for(choice <- choices) {
          createAndUpdateScope(choice.asInstanceOf[SendStatement].label)
          checkAndInitVariables(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].types, choice.asInstanceOf[SendStatement].condition)
          synthMon.handlePayloads(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].types)
          initialWalk(choice.asInstanceOf[SendStatement].continuation)
          curScope = tmpScope
        }

      case RecursiveStatement(label, body) =>
        scopes(curScope).recVariables(label) = body
        initialWalk(body)

      case RecursiveVar(name, continuation) =>
        initialWalk(continuation)

      case End() =>
    }
  }

  /**
   * Traverses the parse tree while invoking the respective methods in SynthMon
   * and SynthProtocol to generate the code.
   *
   * @param statement The root statement of the parse tree.
   */
  def walk(statement: Statement): Unit = {
    statement match {
      case statement @ ReceiveStatement(label, types, condition, continuation) =>
        logger.info("Receive "+label+"("+types+")")
        curScope = defineScopeName(label, getAndUpdateScopeId())
        checkCondition(label, types, condition)

        if(searchScope(label).nonEmpty){
          synthMon.handleReceive(ReceiveStatement(scopes(curScope).scopeId, types, condition, continuation), statement.continuation)
        } else {
          synthMon.handleReceive(statement, statement.continuation)
        }
        synthProtocol.handleReceive(statement, statement.continuation, null)
        walk(statement.continuation)

      case statement @ SendStatement(label, types, condition, _) =>
        logger.info("Send "+label+"("+types+")")
        curScope = defineScopeName(label, getAndUpdateScopeId())
        checkCondition(label, types, condition)

        synthMon.handleSend(statement, statement.continuation)
        synthProtocol.handleSend(statement, statement.continuation, null)
        walk(statement.continuation)

      case statement @ ReceiveChoiceStatement(label, choices) =>
        logger.info("Receive Choice Statement "+label+"{"+choices+"}")
        curScope = defineScopeName(label, getAndUpdateScopeId())
        val tmpScope = curScope
        synthMon.handleReceiveChoice(statement)
        synthProtocol.handleReceiveChoice(statement.label)

        for(choice <- choices) {
          curScope = defineScopeName(choice.asInstanceOf[ReceiveStatement].label, getAndUpdateScopeId())
          checkCondition(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].types, choice.asInstanceOf[ReceiveStatement].condition)
          synthProtocol.handleReceive(choice.asInstanceOf[ReceiveStatement], choice.asInstanceOf[ReceiveStatement].continuation, statement.label)

          walk(choice.asInstanceOf[ReceiveStatement].continuation)
          curScope = tmpScope
        }

      case statement @ SendChoiceStatement(label, choices) =>
        logger.info("Send Choice Statement "+label+"{"+choices+"}")
        curScope = defineScopeName(label, getAndUpdateScopeId())
        val tmpScope = curScope
        synthMon.handleSendChoice(statement)
        synthProtocol.handleSendChoice(statement.label)

        for(choice <- choices) {
          curScope = defineScopeName(choice.asInstanceOf[SendStatement].label, getAndUpdateScopeId())
          checkCondition(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].types, choice.asInstanceOf[SendStatement].condition)

          synthProtocol.handleSend(choice.asInstanceOf[SendStatement], choice.asInstanceOf[SendStatement].continuation, statement.label)
          walk(choice.asInstanceOf[SendStatement].continuation)
          curScope = tmpScope
        }

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

//  def fixContinuation(statement: Statement): Statement = {
//    statement match {
//      case statement @ ReceiveStatement(label, types, condition, continuation) =>
//        if(searchScope(label).nonEmpty) {
//          ReceiveStatement(get right scope of the statement to get its id, types, condition, continuation)
//        }
//    }
//  }

  /**
   * Creates a new scope and adds it to the mapping. The current scope is setup as the
   * parent scope of the new scope.
   *
   * @param label The label of the current statement as the name of the new scope.
   */
  private def createAndUpdateScope(label: String): Unit ={
//    if(scopes.contains(f"${label}_") && scopes(label).parentScope.name == curScope) {
//      throw new Exception("Label " + label + " is already defined in scope.")
//    } else if (scopes.contains(label) && scopes(label).parentScope.name != curScope) {
      //if there is already a label with the same name (not in the same scope)
      //add unique identifier to labels (find a way how to handle this case in walk())
    val tmpScope = searchParentScope(label)
    if (tmpScope != null) {
      throw new Exception("Label " + label + " is already defined in scope: "+ tmpScope.parentScope.scopeId)
    } else {
      val scopeIdentifier = defineScopeName(label, getAndUpdateScopeId())
      scopes(scopeIdentifier) = new Scope(label, scopeIdentifier, scopes(curScope))
      curScope = scopeIdentifier
    }
  }

  private def searchScope(label: String): ListBuffer[Scope] = {
    var tmpScopes = new ListBuffer[Scope]()
    for(scope <- scopes){
      if(scope._2.name == label){
        tmpScopes += scope._2
      }
    }
    tmpScopes
  }

  private def searchParentScope(label: String): Scope = {
    for(scope <- scopes){
      if(scope._2.name == label){
        println(scope._2.parentScope.scopeId, curScope)
        if(scope._2.parentScope.scopeId == curScope){
          return scope._2
        }
      }
    }
    null
  }

  /**
   * Searches for a recursive variable recursively through the scopes. Once found it returns the scope.
   * Otherwise, an exception is thrown indicating that the variable does not exist.
   *
   * @param scope The scope to start searching from.
   * @param recursiveVar The recursive variable to search for.
   * @return The scope of the recursive variable if it exists.
   */
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

  /**
   * Initialises the identifiers within the scope. If the current statement contains a condition,
   * the identifiers within the condition are retrieved and searched for within the previous scopes
   * starting from the current scope. If the identifier is found in another scope a flag is set to
   * indicate that the value it represents shall be used from within other statements.
   *
   * @param label The label of the current statement.
   * @param types A mapping from an identifier to its respective type (representing the payload of
   *              the current statement).
   * @param condition The condition of the current statement.
   */
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

  /**
   * Searches for an identifier recursively through the scopes. Once found it returns the scope.
   * Otherwise, an exception is thrown indicating that the identifier does not exist.
   *
   * @param tmpCurScope The scope from which the search starts.
   * @param identifierName The name of the identifier to be searched.
   * @return The scope of the identifier if it exists.
   */
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

  /**
   * A traverser for traversing scala parse trees.
   */
  class traverser extends Traverser {
    var identifiers: List[String] = List[String]()

    /**
     * Populates a list of identifiers found within a scala parse tree.
     *
     * @param tree The scala parse tree.
     */
    override def traverse(tree: Tree): Unit = tree match {
      case i @ Ident(_) =>
        identifiers = i.name.decodedName.toString :: identifiers
        super.traverse(tree)
      case _ =>
        super.traverse(tree)
    }
  }

  /**
   * Parses a string to obtain a Scala parse tree which is passed to the traverse function in the traverser
   * and returns the distinct identifiers of the traverser excluding util since it represents the file
   * containing any boolean functions used in the condition.
   *
   * @param condition The condition to retrieve identifiers from.
   * @return The distinct identifiers found in the condition.
   */
  def getIdentifiers(condition: String): List[String] = {
    val conditionTree = toolbox.parse(condition)
    val traverser = new traverser
    traverser.traverse(conditionTree)
    traverser.identifiers.distinct.filter(_ != "util")
  }

  /**
   * Type checks a condition of type String using the scala compiler. First, the identifiers are extracted
   * from the condition. Their type is then retrieved and appended to a string as variable declarations. The contents
   * of the util file are extracted as string. The latter, the variable declarations and the condition itself
   * are all appended to a string which is parsed using the Scala parsers and then type-checked using the
   * Scala compiler.
   *
   * @param label The label of the current statement.
   * @param types A mapping from an identifier to its respective type (representing the payload of
   *              the current statement).
   * @param condition The condition to type-check.
   * @return The whether the condition is of type boolean or not.
   */
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

  /**
   * Retrieves the information of an identifier.
   *
   * @param identName The name of the identifier whose information is to be retrieved.
   * @param tmpCurScope The scope from which to start the search.
   * @return A triple consisting of the scope of the identifier, the flag whether it is global or not,
   *         and the type of the identifier.
   */
  def getVarInfo(identName: String, tmpCurScope: String): (String, (Boolean, String)) = {
    (searchIdent(tmpCurScope, identName), scopes(searchIdent(tmpCurScope, identName)).variables(identName))
  }

}
