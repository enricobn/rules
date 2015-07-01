package org.rules.rule

import java.io.File

import org.rules.SwingUI
import org.rules.rule.xml.{XMLProjectFile, XMLProject}

/**
 * Created by enrico on 5/29/15.
 */
object Main {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      val projects = new File("data").listFiles().filter(_.isDirectory).map(_.getName)
      SwingUI.choose("Projects", "choose project", projects.toList) match {
        case Some(projectName) => openProject(new File(new File("data"), projectName))
        case _ =>
      }
    } else {
      openProject(new File(args(0)))
    }
  }

  private def openProject(file: File) =
    XMLProjectFile.open(file) match {
      case Logged(Some(project), _) => project.xmlProject.solver(SwingUI).run()
      case Logged(None, messages) => messages
    }

}
