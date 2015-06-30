package org.rules.lift.snippet

import net.liftweb.http.SessionVar
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds._
import org.rules.lift.RulesDAOProvider

import scala.xml.Text

/**
 * Created by enrico on 6/1/15.
 */
object RulesState extends RulesDAOProvider {
  private object projectVar extends SessionVar[Option[String]](None)
  // TOD better if it's only an id
  private object moduleVar extends SessionVar[Option[String]](None)

  def currentProjectName = projectVar.get

  def currentModuleName = moduleVar.get

  def setCurrentProjectName(name: String) = projectVar.set(Some(name))

  def setCurrentModuleName(name: String) = moduleVar.set(Some(name))

  /**
   * TODO I don't like it
   * it checks if the deleted project is the current one, if true returns a JsCmd which repaints project-menu
   * and content
   * @param name
   */
  def projectDeleted(name: String) =
    projectVar.get match {
      case Some(projectName) if projectName == name =>
        projectVar.set(None)

        SetHtml("project-menu", Text("")) &
        SetHtml("content", Text("")) &
        Run("pack();")
      case _ => Noop
    }

  /**
   * TODO I don't like it
   * it checks if the deleted module is the current one, if true returns a JsCmd which repaints the content
   * @param name
   */
  def moduleDeleted(name: String) =
    moduleVar.get match {
      case Some(moduleName) if moduleName == name =>
        moduleVar.set(None)

        SetHtml("content", Text("")) &
        Run("pack();")
      case _ => Noop
    }
}
