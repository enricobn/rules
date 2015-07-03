package org.rules.lift.model

import java.io.File

import net.liftweb.common.{Full, Failure, Box}
import org.rules.rule.Logged
import org.rules.rule.xml.{XMLModuleFile, XMLProjectFile, XMLRule}

/**
 * Created by enrico on 6/24/15.
 */
object RulesXMLFileDAO extends RulesDAO {
  private val root = new File("data")
  // TODO cache and update on changes
  private def fileProjects = root.listFiles().filter(_.isDirectory).sortWith(_.getName < _.getName)
    .map(XMLProjectFile.open(_)).collect{case Logged(Some(p), _) => p}

  def getProjects = fileProjects.map(_.xmlProject)

  def createProject(name: String) = new File(root, name).mkdir()

  def delProject(name: String) =
    findProject(name, (projectFile) => {projectFile.delete(); Full({})})

  private def findProject[T](name: String, func: (XMLProjectFile) => Box[T]) : Box[T] =
    fileProjects.find(_.name == name) match {
      case Some(projectFile) => func(projectFile)
      case _ => Failure(s"""Cannot find project "$name"""")
    }

  private def findModule[T](projectName: String, name: String,
        func: (XMLProjectFile, XMLModuleFile) => Box[T]) : Box[T] = {
    findProject[T](projectName, (projectFile) => {
      projectFile.xmlModulesFiles.find(_.name == name) match {
        case Some(moduleFile) => func(projectFile, moduleFile)
        case _ => Failure(s"""Cannot find module "$name" in project "$projectName"""")
      }
    })
  }

  def createModule(projectName: String, name: String) : Box[Unit] =
    findProject(projectName, (projectFile) => {projectFile.createModule(name); Full({})})

  def delModule(projectName: String, name: String) : Box[Unit] =
    findProject(projectName, (projectFile) => {
      projectFile.delModule(name) match {
        case Some(newProjectFile) => Full({})
        case _ => Failure(s"""Cannot find module "$name"""")
      }
    })

  def updateRuleAndSave(projectName: String, moduleName: String, rules: Seq[XMLRule]) =
    findModule(projectName, moduleName, (projectFile, moduleFile) => {moduleFile.updateAndSave(rules); Full({})} )

  def getRules(projectName: String, moduleName: String) =
    findModule(projectName, moduleName, (projectFile, moduleFile) => Full(moduleFile.xmlModule.rules) )

  def getModules(projectName: String) =
    findProject(projectName, (projectFile) => Full(projectFile.xmlModulesFiles.map(_.xmlModule)))

  def getProject(name: String) = findProject(name, (projectFile) => Full(projectFile.xmlProject))

}