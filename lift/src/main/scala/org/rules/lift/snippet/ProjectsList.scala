package org.rules.lift.snippet

import java.io.File
import java.util.UUID

import net.liftweb.common.Loggable
import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.{JsRaw, Str, JsVar}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.CssSelectorParser
import org.rules.lift.RulesDAOProvider
import net.liftweb.util.Helpers._
import org.rules.lift.model.RulesDAO
import org.rules.lift.utils.{JQueryTabs, LiftUtils}
import org.rules.rule.Logged
import org.rules.rule.xml.{XMLProjectFile, XMLProject}
import net.liftweb.json.JsonDSL._


/**
 * Created by enrico on 6/25/15.
 */
object ProjectsList extends Loggable with RulesDAOProvider with JQueryTabs {

  private def updateProjectMenu(projectName: String) : JsCmd =
    //val newTabId = UUID.randomUUID().toString
    LiftUtils.getOrElseError[XMLProject,JsCmd](
      rulesDAO.getProject(projectName),
      (project) => addTab("projects-tabs", projectName, () => <lift:embed what="/project-menu" projectName={projectName}/>,
        (name, contentId) => Run(
          s"""
             |var r;
             |${JsIf(ProjectMenu.hasUnsavedChanges(projectName),
                 Run("r = confirm('There are pending changes. Close anyway?');"),
                 Run("r = true;")
               ).toJsCmd
             }
             |if (r) {
             |  ${ProjectMenu.clearProject(projectName).toJsCmd}
             |}
             |return r;
          """.stripMargin
        )
      ),
      s"""Failed to load project "$projectName"""",
      Noop)

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
    S.appendJs(Run("""$('[data-toggle="tooltip"]').tooltip();""") &
      createTabs("content", "projects-tabs", "heightStyle" -> "fill")
    )

    "#projects-list-container *" #> renderProjects &
    "#add-project [onClick]" #> addProject()
  }
}
