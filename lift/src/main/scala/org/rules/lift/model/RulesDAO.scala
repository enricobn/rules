package org.rules.lift.model

import java.io.File

import net.liftweb.common.Box
import org.rules.rule.Logged
import org.rules.rule.xml._

/**
 * Created by enrico on 6/24/15.
 */
trait RulesDAO {
  def getProjects : Seq[XMLProject]

  def createProject(name: String) : Unit

  def delProject(name: String) : Box[Unit]

  def createModule(projectName: String, name: String) : Box[Unit]

  def delModule(projectName: String, name: String) : Box[Unit]

  def updateRulesAndSave(projectName: String, moduleName: String, rules: Seq[XMLRule], deletedRulesIds: Seq[String]) : Box[Unit]

  def getRules(projectName: String, moduleName: String) : Box[Seq[XMLRule]]

  def getModules(projectName: String) : Box[Seq[XMLModule]]

  def getProject(name: String) : Box[XMLProject]

  def createRule(projectName: String, moduleName: String, name: String) : XMLRule

}
