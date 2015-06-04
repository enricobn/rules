package org.rules.rule

import org.rules.SwingUI
import org.scalatest._

class ExampleRulesSpec extends FlatSpec {
  
  val rules = Set(Oracle, OracleByCatalog, SQLServer, SQLServerByCatalog, SQLServerDev, 
      RepositoryDevCatalog, MainDevCatalog, ConnectionRule, Goal, DummyVersionRule, OracleDev,
      RepositoryCons, SQLServerCons2005, SQLServerCons2008, MainCons, SQLServerConnectionFunction, OracleConnectionFunction
  )
  
  val solver = new RuleSolver[String](Set(GoalFactory), rules, SwingUI)

  "run" must "be valid" in {
    solver.tree.value match {
      case Some(tree) =>
        println(tree.pretty)
        assert(solver.run().isEmpty)
      case None => fail(solver.tree.messages.toString())
    }
  }

}