package org.rules.lift.model

import java.io.File

import org.rules.rule.Logged
import org.rules.rule.xml._

/**
 * Created by enrico on 6/24/15.
 */
trait RulesDAO {
  def getProjects : Seq[XMLProject]

  def createProject(name: String) : Unit

  def delProject(name: String) : Logged[Boolean]

  def createModule(projectName: String, name: String) : Logged[Boolean]

  def delModule(projectName: String, name: String) : Logged[Boolean]

  def updateRuleAndSave(projectName: String, moduleName: String, rules: Seq[XMLRule]) : Logged[Boolean]

  def getRules(projectName: String, moduleName: String) : Logged[Seq[XMLRule]]

  def getModules(projectName: String) : Logged[Seq[XMLModule]]

  def getProject(name: String) : Logged[XMLProject]

}
