package monitor.model

import scala.collection.mutable

class Scope(val name: String, val id: String, val parentScope: Scope) {
  var recVariables = new mutable.HashMap[String, Statement]
  var variables = new mutable.HashMap[String, (Boolean, String)] // name => (global, type)
}