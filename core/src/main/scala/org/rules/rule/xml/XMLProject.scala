package org.rules.rule.xml

import java.io.File
import java.util.UUID

import org.rules.UI
import org.rules.rule._
import org.rules.utils.Utils

/**
 * Created by enrico on 6/7/15.
 */

case class XMLProject(name: String, modules: Set[XMLModule]) {
  val factories = modules.flatMap(_.factories)
  val rules = modules.flatMap(_.rules)

  def solver(ui: UI) = new RuleSolver[String](factories.asInstanceOf[Set[RuleFactory[String]]],
    rules.asInstanceOf[Set[Rule[String]]], ui)

}
