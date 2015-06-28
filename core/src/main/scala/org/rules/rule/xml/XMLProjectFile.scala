package org.rules.rule.xml

import java.io.File
import java.util.UUID

import org.rules.rule.{xml, Logged}
import org.rules.utils.Utils

/**
 * Created by enrico on 6/27/15.
 */
object XMLProjectFile {
  def open(folder: File) : Logged[XMLProjectFile] = {
    val files = Utils.recursiveListFiles(folder, (f) => f.getName.endsWith(".rules.xml"))

    files.value match {
      case Some(good) => XMLProjectFile(folder, createModulesFiles(good))
      case _ => files.messages.toString()
    }
  }

  private def createModulesFiles(files: Seq[File]) = {
    var count = 0
    files.map (XMLModuleFile(/*{count += 1; count}.toString, */_))
  }

}
case class XMLProjectFile(folder: File, xmlModulesFiles: Seq[XMLModuleFile]) {
  val name = folder.getName
  val xmlProject = XMLProject(name, xmlModulesFiles.map(_.xmlModule).toSet)

  def updateModule(moduleFile: XMLModuleFile) = {
    xml.XMLProjectFile(folder, xmlModulesFiles.map{ m => if (m.file == moduleFile.file) moduleFile else m})
  }

  def createModule(name: String) = {
    def file = new File(folder, name + ".rules.xml")
    def module = XMLModule(name, Seq.empty, Seq.empty)
    module.save(file)
    xml.XMLProjectFile(folder, xmlModulesFiles.+:(XMLModuleFile(/*UUID.randomUUID().toString,*/ file)))
  }

  def delModule(name: String) = {
    def module = xmlModulesFiles.find(_.name == name)
    module match {
      case Some(m) =>
        m.delete()
        Some(xml.XMLProjectFile(folder, xmlModulesFiles.filter(_.name != name)))
      case _ => None
    }
    /*    def file = new File(folder, name + ".rules.xml")
        def module = XMLModule(name, Seq.empty, Seq.empty)
        module.save(file)
        XMLProjectFile(folder, xmlModulesFiles.+:(XMLModuleFile(UUID.randomUUID().toString, file)))
        */
  }

  def delete() = Utils.delete(folder)
}


