package org.rules.lift.snippet

import java.io.File

import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import org.rules.lift.RulesDAO
import net.liftweb.util.Helpers._
import org.rules.rule.xml.XMLProject

import scala.xml.{NodeSeq, Text}

/**
 * Created by enrico on 6/25/15.
 */
object ProjectsList {
  
  private def updateProjectMenu(folder: File) : JsCmd = {
    val ifProject = XMLProject(folder)

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

  def showProjectName() = {
    SetValById("add-project-name", "new project") &
    JsShowId("add-project-name") &
    Run("pack();")
  }

  def addProject(name: String) = {
    if (name.isEmpty) {
      JsHideId("add-project-name") &
      Run("pack();")
    } else {
      RulesDAO.addProject(name)

      JsHideId("add-project-name") &
      SetHtml("projects-list-container  ", renderProjectsVar.is.get.applyAgain()) &
      Run("pack();")
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
        Run(s"""bootbox.confirm("Are you sure?", function(result) {
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
    "#add-project [onClick]" #> showProjectName &
    "#add-project-name [style+]" #> "display: none;" &
    "#add-project-name [onchange]" #> (onEvent(addProject))
  }
}
