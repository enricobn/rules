package org.rules.lift.snippet

import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.{S, MemoizeTransform, RequestVar, SHtml}
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.model.RulesDAO
import org.rules.rule.Logged
import org.rules.rule.xml.{XMLModuleFile, XMLProjectFile}

import scala.xml.Text

/**
 * Created by enrico on 6/22/15.
 * project-menu.html
 */
object ProjectMenu extends RulesDAOProvider {
  private val modulesFinder = JQueryById("modules-buttons")
  private val moduleGroup : JQueryGroup = new JQueryGroup(modulesFinder, JQueryHide)

  // TODO error check
  private def modules = rulesDAO.getModules(Index.currentProjectName.get).value.get

  private def updateModule(name: String) = {
    val deselect = Index.currentModuleName match {
      case Some(module) => moduleGroup.deSelect(module)
      case _ => Noop
    }
    Index.setCurrentModuleName(name)

    // TODO cleanup of resources
    Run(SetHtml("content", Text(""))) &
      deselect &
      moduleGroup.select(name) &
      Run("pack();")
    //Run(js)
  }

  private def addModuleCall(name: String) = {
    if (!name.isEmpty) {
      // TODO check errors
      rulesDAO.createModule(Index.currentProjectName.get, name)

      //Index.projectVar.set(Some(newProject))

      SetHtml("modules-list-container", renderModulesVar.is.get.applyAgain()) &
      Run("pack();")
    } else {
      Noop
    }
  }

  private def addModule() = {
    /*    SetValById("add-project-name", "new project") &
        JsShowId("add-project-name") &
        Run("pack();")
    */
    Run(s"""bootbox.prompt("Module name", function(result) {
          if(result != null) {
            ${ajaxCall(JsVar("result"), addModuleCall)};
          }
        });
        """)
  }

  private def delModule(id: String) = {
    val newProject = rulesDAO.delModule(Index.currentProjectName.get, id)

    newProject match {
      case Logged(Some(p), _) =>
        //Index.projectVar.set(Some(p))

        SetHtml("modules-list-container  ", renderModulesVar.is.get.applyAgain()) &
          Index.moduleDeleted(id) &
          Run("pack();")
      case Logged(None, msgs) => S.error("Cannot delete module: " + msgs)
        Noop
    }
  }

  private val renderModules = SHtml.memoize(
    "#modules-list *" #> modules.map(module =>
      ".select-module [onClick]" #> ajaxInvoke(() => updateModule(module.name)) &
      ".select-module *" #> module.name &
      ".list-rules [onClick]" #> ajaxInvoke(() => Index.updateRules()) &
      ".modules-buttons [id]" #> modulesFinder.getJQueryId(module.name) &
      ".modules-buttons [style+]" #> "display: none;" &
      ".del-module [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete module ${module.name}?",
          () => delModule(module.name))
    )
  )

  private object renderModulesVar extends RequestVar[Option[MemoizeTransform]](None)

  def render = {
    renderModulesVar.set(Some(renderModules))

    "#modules-list-container *" #> renderModules &
    "#project-name *" #> Index.currentProjectName.get &
    "#add-module [onClick]" #> addModule()
  }
}
