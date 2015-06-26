package org.rules.lift.snippet

import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.{S, MemoizeTransform, RequestVar, SHtml}
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.rule.xml.{XMLModuleFile, XMLProjectFile}

import scala.xml.Text

/**
 * Created by enrico on 6/22/15.
 * project-menu.html
 */
object ProjectMenu {
  val modulesFinder = JQueryById("modules-buttons")
  val moduleGroup : JQueryGroup = new JQueryGroup(modulesFinder, JQueryHide)

  def modules = Index.projectVar.get.get.xmlModulesFiles

  def updateModule(id: String) = {
    val deselect = Index.moduleVar.get match {
      case Some(module) => moduleGroup.deSelect(module.id)
      case _ => Noop
    }
    Index.moduleVar.set(modules.find(_.id == id))

    // TODO cleanup of resources
    Run(SetHtml("content", Text(""))) &
      deselect &
      moduleGroup.select(id) &
      Run("pack();")
    //Run(js)
  }

  def addModuleCall(name: String) = {
    if (!name.isEmpty) {
      val newProject = RulesDAO.addModule(Index.projectVar.get.get, name)

      Index.projectVar.set(Some(newProject))

      SetHtml("modules-list-container", renderModulesVar.is.get.applyAgain()) &
      Run("pack();")
    } else {
      Noop
    }
  }

  def addModule() = {
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

  def delModule(id: String) = {
    val newProject = RulesDAO.delModule(Index.projectVar.get.get, id)

    newProject match {
      case Some(p) =>
        Index.projectVar.set(Some(p))

        SetHtml("modules-list-container  ", renderModulesVar.is.get.applyAgain()) &
          Index.moduleDeleted(id) &
          Run("pack();")
      case _ => S.notice("Cannot delete module!")
        Noop
    }
  }

  val renderModules = SHtml.memoize(
    "#modules-list *" #> modules.map(module =>
      ".select-module [onClick]" #> ajaxInvoke(() => updateModule(module.id)) &
      ".select-module *" #> module.xmlModule.name &
      ".list-rules [onClick]" #> ajaxInvoke(() => Index.updateRules()) &
      ".modules-buttons [id]" #> modulesFinder.getJQueryId(module.id) &
      ".modules-buttons [style+]" #> "display: none;" &
      ".del-module [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete module ${module.xmlModule.name}?",
          () => delModule(module.id))
    )
  )

  private object renderModulesVar extends RequestVar[Option[MemoizeTransform]](None)

  def render = {
    renderModulesVar.set(Some(renderModules))

    "#modules-list-container *" #> renderModules &
    "#project-name *" #> Index.projectVar.get.get.name &
    "#add-module [onClick]" #> addModule()
  }
}
