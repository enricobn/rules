package org.rules.lift.snippet

import java.io.File

import net.liftweb.common.Loggable
import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.CssSelectorParser
import org.rules.lift.{RulesDAOProvider, LiftUtils}
import net.liftweb.util.Helpers._
import org.rules.lift.model.RulesDAO
import org.rules.rule.Logged
import org.rules.rule.xml.{XMLProjectFile, XMLProject}

import scala.xml.{NodeSeq, Text}

/**
 * Created by enrico on 6/25/15.
 */
object ProjectsList extends Loggable with RulesDAOProvider {
  
  private def updateProjectMenu(projectName: String) : JsCmd = {
    LiftUtils.getOrElseError[XMLProject,JsCmd](
      rulesDAO.getProject(projectName),
      (project) => {
        RulesState.setCurrentProjectName(project.name)
        SetHtml("project-menu", <span class="lift:embed?what=/project-menu" />) &
        SetHtml("content", Text("")) &
        Run("pack();")
      },
      s"""Failed to load project "$projectName"""",
      Noop)
  }

  private def addProject() = {
    LiftUtils.bootboxPrompt("Project name", addProjectByName)
  }

  private def addProjectByName(name: String) = {
    if (!name.isEmpty) {
      rulesDAO.createProject(name)

      SetHtml("projects-list-container", renderProjectsVar.is.get.applyAgain()) &
      Run("pack();")
    } else {
      Noop
    }
  }

  private def delProject(name: String) = {
    rulesDAO.delProject(name)

    RulesState.projectDeleted(name) &
    SetHtml("projects-list-container", renderProjectsVar.is.get.applyAgain()) &
    Run("pack();")
  }

  private val renderProjects = SHtml.memoize {
    "#projects-list-elements *" #> rulesDAO.getProjects.map{ project =>
      ".select-project [onClick]" #> ajaxInvoke(() => updateProjectMenu(project.name)) &
        ".select-project *" #> project.name &
        ".del-project [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete project ${project.name}?",
          () => delProject(project.name))
    }
  }

  private object renderProjectsVar extends RequestVar[Option[MemoizeTransform]](None)

  def render() = {
    renderProjectsVar.set(Some(renderProjects))

    "#projects-list-container *" #> renderProjects &
    "#add-project [onClick]" #> addProject()
  }
}
