package org.rules.lift.snippet

import java.io.File

import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import org.rules.lift.RulesDAO
import net.liftweb.util.Helpers._
import org.rules.rule.xml.{XMLProjectFile, XMLProject}

import scala.xml.{NodeSeq, Text}

/**
 * Created by enrico on 6/25/15.
 */
object ProjectsList {
  
  private def updateProjectMenu(folder: File) : JsCmd = {
    val ifProject = XMLProjectFile.create(folder)

    if (ifProject.value.isEmpty) {
      return Run("alert('Failed to load project');")
    }

    val p = ifProject.value.get

    Index.projectVar.set(Some(p))

    SetHtml("project-menu", <span class="lift:embed?what=/project-menu" />) &
    SetHtml("content", Text("")) &
    Run("pack();")

    /*    CmdPair(
          updateProjectMenu(p),
          SetHtml("content", Text(""))
        )
    */
    /*    projectVar.set(Some(p))
    
        CmdPair(
          CmdPair(
            SetHtml("project-menu", <span class="lift:embed?what=/project-menu" />
      /*
              <h2 class="rules-nav">{p.name}</h2> ++
                modules()
    */
              //ajaxButton("Rules", () => updateRules()) ++
              //<br></br> ++
              //ajaxButton("Factories", () => updateFactories())
            ),
            SetHtml("content", Text(""))),
          Run("pack();")
        )
        */
  }

  def addProject() = {
/*    SetValById("add-project-name", "new project") &
    JsShowId("add-project-name") &
    Run("pack();")
*/
    Run(s"""bootbox.prompt("Project name", function(result) {
          if(result != null) {
            ${ajaxCall(JsVar("result"), addProjectCall)};
          }
        });
        """)
  }

  def addProjectCall(name: String) = {
    if (!name.isEmpty) {
      RulesDAO.addProject(name)

      SetHtml("projects-list-container  ", renderProjectsVar.is.get.applyAgain()) &
      Run("pack();")
    } else {
      Noop
    }
  }

  def delProject(folder: File) = {
      RulesDAO.delete(folder)

      SetHtml("projects-list-container  ", renderProjectsVar.is.get.applyAgain()) &
      Run("pack();")
  }

  private val renderProjects = SHtml.memoize {
    "#projects-list-elements *" #> RulesDAO.projects.map(folder =>
      ".select-project [onClick]" #> ajaxInvoke(() => updateProjectMenu(folder)) &
      ".select-project *" #> folder.getName &
      ".del-project [onClick]" #>
        Run(s"""bootbox.confirm("Are you sure to delete project ${folder.getName}?", function(result) {
          if(result) {
            ${ajaxInvoke(() => delProject(folder))};
          }
        });
        """)
    )
  }

  private object renderProjectsVar extends RequestVar[Option[MemoizeTransform]](None)

  def render() = {
    renderProjectsVar.set(Some(renderProjects))

    "#projects-list-container *" #> renderProjects &
    "#add-project [onClick]" #> addProject()
  }
}
