package monitor.interpreter

import monitor.model._
import monitor.model.Scope
import monitor.synth.{SynthMon, SynthProtocol}

import scala.collection.mutable
import scala.reflect.runtime._
import scala.tools.reflect.ToolBox

class STInterpreter(sessionType: SessionType, path: String, preamble: String, method: String) {
  private val toolbox = currentMirror.mkToolBox()

  private var scopes = new mutable.HashMap[String, Scope]()
  private var curScope = "global"
  scopes(curScope) = new Scope(curScope, curScope, null)

  private var branches = new mutable.HashMap[Statement, String]()

  var synthMon = new SynthMon(this, path)
  var synthProtocol = new SynthProtocol(this, path)

  def getRecursiveVarScope(recursiveVar: RecursiveVar): Scope = {
    checkRecVariable(scopes(curScope), recursiveVar)
  }

  def getBranchLabel(statement: Statement): String = {
    branches(statement)
  }

  def getCurScope: String ={
    curScope
  }

  def getScopes: mutable.HashMap[String, Scope] = {
    scopes
  }

  /**
   * Starts the interpreter and invokes the initialisation methods of SynthMon and SynthProtocol.
   *
   * @return A tuple consisting of two string builders in which the generated code for
   *         the monitor and cpsp classes is found.
   */
  def run(): (StringBuilder, StringBuilder) = {
//    sessionType.statement match {
//      case recursiveStatement: RecursiveStatement =>
//        var tmpStatement: Statement = recursiveStatement.body
//        while(tmpStatement.isInstanceOf[RecursiveStatement]){
//          tmpStatement = recursiveStatement.body
//        }
//        synthMon.startInit(preamble)
//      case _ =>
//        synthMon.startInit(preamble)
//    }

    synthMon.startInit(preamble)
    initialWalk(sessionType.statement)
    curScope = "global"
    synthMon.endInit()

    synthProtocol.init(preamble)
    walk(sessionType.statement)

    curScope = "global"
    probabilityWalk(sessionType.statement)
    synthMon.addCalculateInterval(method)
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
      case ReceiveStatement(label, statementID, types, probBoundary, continuation) =>
        createAndUpdateScope(label, statementID)
        synthMon.handleLabels(statementID, probBoundary.probability, choice = false)
        initialWalk(continuation)

      case SendStatement(label, statementID, types, probBoundary, continuation) =>
        createAndUpdateScope(label, statementID)
        synthMon.handleLabels(statementID, probBoundary.probability, choice = false)
        initialWalk(continuation)

      case ReceiveChoiceStatement(label, choices) =>
        createAndUpdateScope(label, label)
        val tmpScope = curScope
        synthMon.handleLabels(label, 0, choice = true)
        for(choice <- choices) {
          createAndUpdateScope(choice.asInstanceOf[ReceiveStatement].label, choice.asInstanceOf[ReceiveStatement].statementID)
          synthMon.handleLabels(choice.asInstanceOf[ReceiveStatement].statementID, choice.asInstanceOf[ReceiveStatement].probBoundary.probability, choice = false)
          initialWalk(choice.asInstanceOf[ReceiveStatement].continuation)
          curScope = tmpScope
        }

      case SendChoiceStatement(label, choices) =>
        createAndUpdateScope(label, label)
        val tmpScope = curScope
        synthMon.handleLabels(label, 0, choice = true)
        for(choice <- choices) {
          createAndUpdateScope(choice.asInstanceOf[SendStatement].label, choice.asInstanceOf[SendStatement].statementID)
          synthMon.handleLabels(choice.asInstanceOf[SendStatement].statementID, choice.asInstanceOf[SendStatement].probBoundary.probability, choice = false)
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
      case statement @ ReceiveStatement(label, id, types, probBoundary, _) =>
        curScope = id
        if(probBoundary.probability!=1 && probBoundary.probability!=0){
          throw new Exception("Probability in " + label + " is not 1 nor ignored")
        }
        synthMon.handleReceive(statement, statement.continuation, scopes(curScope).isUnique) // Change isUnique accordingly
        synthProtocol.handleReceive(statement, scopes(curScope).isUnique, statement.continuation, getScope(statement.continuation).isUnique, null)
        walk(statement.continuation)

      case statement @ SendStatement(label, id, types, probBoundary, _) =>
        curScope = id
        if(probBoundary.probability!=1 && probBoundary.probability!=0){
          throw new Exception("Probability in " + label + " is not 1 nor ignored")
        }
        synthMon.handleSend(statement, statement.continuation, scopes(curScope).isUnique) // Change isUnique accordingly
        synthProtocol.handleSend(statement, scopes(curScope).isUnique, statement.continuation, getScope(statement.continuation).isUnique, null)
        walk(statement.continuation)

      case statement @ ReceiveChoiceStatement(label, choices) =>
        curScope = label
        val tmpScope = curScope
        synthMon.handleReceiveChoice(statement)
        synthProtocol.handleReceiveChoice(statement.label)
        var totalProb=0.0

        for(choice <- choices) {
          curScope = choice.asInstanceOf[ReceiveStatement].statementID
          totalProb+=choice.asInstanceOf[ReceiveStatement].probBoundary.probability
          if(totalProb > 1){
            throw new Exception("Probabilities in " + label + " exceed 1")
          }
          synthProtocol.handleReceive(choice.asInstanceOf[ReceiveStatement], scopes(curScope).isUnique, choice.asInstanceOf[ReceiveStatement].continuation, getScope(choice.asInstanceOf[ReceiveStatement].continuation).isUnique, statement.label)

          walk(choice.asInstanceOf[ReceiveStatement].continuation)
          curScope = tmpScope
        }

      case statement @ SendChoiceStatement(label, choices) =>
        curScope = label
        val tmpScope = curScope
        synthMon.handleSendChoice(statement)
        synthProtocol.handleSendChoice(statement.label)
        var totalProb=0.0

        for(choice <- choices) {
          curScope = choice.asInstanceOf[SendStatement].statementID
          totalProb+=choice.asInstanceOf[SendStatement].probBoundary.probability
          if(totalProb > 1){
            throw new Exception("Probabilities in " + label + " exceed 1")
          }

          synthProtocol.handleSend(choice.asInstanceOf[SendStatement], scopes(curScope).isUnique, choice.asInstanceOf[SendStatement].continuation, getScope(choice.asInstanceOf[SendStatement].continuation).isUnique, statement.label)
          walk(choice.asInstanceOf[SendStatement].continuation)
          curScope = tmpScope
        }

      case statement @ RecursiveStatement(label, body) =>
        walk(statement.body)

      case statement @ RecursiveVar(name, continuation) =>
        checkRecVariable(scopes(curScope), statement)
        walk(statement.continuation)

      case End() =>

      }
  }

  def probabilityWalk(statement: Statement): Unit = {
    statement match {
      case statement@ReceiveStatement(_, id, _, _, _) =>
        curScope = id
        probabilityWalk(statement.continuation)

      case statement@SendStatement(_, id, _, _, _) =>
        curScope = id
        probabilityWalk(statement.continuation)

      case statement @ ReceiveChoiceStatement(label, choices) =>
        curScope = label
        val tmpScope = curScope
        synthMon.handleReceiveChoiceInterval(statement)
        for (choice <- choices) {
          curScope = choice.asInstanceOf[ReceiveStatement].statementID
          probabilityWalk(choice.asInstanceOf[ReceiveStatement].continuation)
          curScope = tmpScope
        }

      case statement@SendChoiceStatement(label, choices) =>
        curScope = label
        val tmpScope = curScope
        synthMon.handleSendChoiceInterval(statement)
        for (choice <- choices) {
          curScope = choice.asInstanceOf[SendStatement].statementID
          probabilityWalk(choice.asInstanceOf[SendStatement].continuation)
          curScope = tmpScope
        }

      case statement@RecursiveStatement(label, body) =>
        probabilityWalk(statement.body)

      case statement@RecursiveVar(name, continuation) =>
        probabilityWalk(statement.continuation)

      case End() =>

    }
  }

  /**
   * Creates a new scope and adds it to the mapping. The current scope is setup as the
   * parent scope of the new scope.
   *
   * @param label The label of the current statement as the name of the new scope.
   */
  private def createAndUpdateScope(label: String, id: String): Unit ={
    if (searchParentScope(label) != null) {
      throw new Exception("Label " + label + " is already defined in scope")
    } else {
      val tmpScopes = searchScope(label)
      if(tmpScopes.isEmpty){
        scopes(id) = new Scope(label, id, scopes(curScope))
      } else {
        scopes(id) = new Scope(label, id, scopes(curScope))
        scopes(id).isUnique = false
        for(tmpScope <- tmpScopes){
          scopes(tmpScope.id).isUnique = false
//          new Scope(tmpScope.name, tmpScope.id, tmpScope.parentScope, false)
        }
      }
      curScope = id
    }
  }

  def getScope(scopeName: String): Scope = {
    scopes(scopeName)
  }

  def getScope(statement: Statement): Scope = {
    statement match {
      case ReceiveStatement(label, statementID, types, condition, continuation) =>
        scopes(statementID)

      case SendStatement(label, statementID, types, condition, continuation) =>
        scopes(statementID)

      case ReceiveChoiceStatement(label, choices) =>
       scopes(label)

      case SendChoiceStatement(label, choices) =>
        scopes(label)

      case RecursiveStatement(label, body) =>
        getScope(body)

      case RecursiveVar(name, continuation) =>
        getScope(continuation)

      case End() => scopes(curScope)
    }
  }

  private def searchParentScope(label: String): Scope = {
    for(scope <- scopes){
      if(scope._2.name == label){
        if(scope._2.parentScope.name == curScope){
          return scope._2
        }
      }
    }
    null
  }

  def searchScope(label: String): mutable.ListBuffer[Scope] = {
    var tmpScopes = new mutable.ListBuffer[Scope]()
    for(scope <- scopes){
      if(scope._2.name == label){
        tmpScopes += scope._2
      }
    }
    tmpScopes
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
      if(!scopes(scope.id).recVariables.contains(recursiveVar.name)){
        checkRecVariable(scopes(scope.id).parentScope, recursiveVar)
      } else {
        scope
      }
    } else {
      throw new Exception("Error: Recursive variable "+recursiveVar.name+" not defined.")
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
      searchIdent(scopes(tmpCurScope).parentScope.id, identifierName)
    } else {
      tmpCurScope
    }
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
