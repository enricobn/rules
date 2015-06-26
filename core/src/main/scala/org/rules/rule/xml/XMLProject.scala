package org.rules.rule.xml

import java.io.File
import java.util.UUID

import org.rules.UI
import org.rules.rule._
import org.rules.utils.Utils

/**
 * Created by enrico on 6/7/15.
 */

object XMLProjectFile {
  def create(folder: File) : Logged[XMLProjectFile] = {
    val files = Utils.recursiveListFiles(folder, (f) => f.getName.endsWith(".rules.xml"))

    files.value match {
      case Some(good) => XMLProjectFile(folder, createModulesFiles(good))
      case _ => files.messages.toString()
    }
  }

  private def createModulesFiles(files: Seq[File]) = {
    var count = 0
    files.map (XMLModuleFile({count += 1; count}.toString, _))
  }

}

case class XMLProjectFile(folder: File, xmlModulesFiles: Seq[XMLModuleFile]) {
  val name = folder.getName
  val xmlProject = XMLProject(name, xmlModulesFiles.map(_.xmlModule).toSet)

  def updateModule(moduleFile: XMLModuleFile) = {
    XMLProjectFile(folder, xmlModulesFiles.map{ m => if (m.file == moduleFile.file) moduleFile else m})
  }

  def createModule(name: String) = {
    def file = new File(folder, name + ".rules.xml")
    def module = XMLModule(name, Seq.empty, Seq.empty)
    module.save(file)
    XMLProjectFile(folder, xmlModulesFiles.+:(XMLModuleFile(UUID.randomUUID().toString, file)))
  }

}

case class XMLProject(name: String, modules: Set[XMLModule]) {
  val factories = modules.flatMap(_.factories)
  val rules = modules.flatMap(_.rules)

  def solver(ui: UI) = new RuleSolver[String](factories.asInstanceOf[Set[RuleFactory[String]]],
    rules.asInstanceOf[Set[Rule[String]]], ui)

}
