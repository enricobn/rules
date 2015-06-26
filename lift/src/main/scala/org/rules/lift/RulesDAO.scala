package org.rules.lift

import java.io.File

import org.rules.rule.xml.{XMLModuleFile, XMLProjectFile, XMLProject}
import org.rules.utils.Utils

/**
 * Created by enrico on 6/24/15.
 */
object RulesDAO {
  private val root = new File("data")
  def projects = root.listFiles().filter(_.isDirectory).sortWith(_.getName < _.getName)
    .map(XMLProjectFile.open(_))

  def addProject(name: String) = new File(root, name).mkdir()

  def delProject(project: XMLProjectFile) = project.delete()

  def addModule(project: XMLProjectFile, name: String) = project.createModule(name)

  def delModule(project: XMLProjectFile, id: String) = project.delModule(id)
}
