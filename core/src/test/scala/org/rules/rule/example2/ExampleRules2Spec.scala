package org.rules.rule.example2

import org.rules.{SwingUI, UI}
import org.rules.rule.{RuleFactory, RuleSolver, Rule}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class ExampleRules2Spec extends FlatSpec with MockFactory {

  val ui = stub[UI]

  "run" must "be valid" in {
    val rules = Set(Goal, Main, Repo, Main1, Repo1, Connection)

    val solver = new RuleSolver(Set.empty[RuleFactory[String]], rules, ui)

    val stubui = ChooserStub(solver)
    stubui.add(List(Main,Main1), Main1)
    stubui.add(List(Repo,Repo1), Repo)
    
    // test expectations


    assert(solver.run().isEmpty)
  }
  
  case class ChooserStub(solver: RuleSolver[String]) {

    def add(when: List[Rule[String]], thenReturn: Rule[String]) = {
      (ui.choose[solver.RuleTree] _).when(whereList(solver, when))
        .onCall({ (title: String, message: String, list: List[solver.RuleTree]) => 
          list.find { x => x.rule == thenReturn }
        })
    }
  }
  
  def whereList(solver: RuleSolver[String], when: List[Rule[String]]) = {
    where { (title: String, message: String, list: List[solver.RuleTree]) =>
      val s = list.map { x => x.rule }
      s.toSet == when.toSet 
    }
  }
  
}