package org.rules.lift.snippet

import java.util.UUID

import net.liftweb.common.Full
import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.js.JsCmd
import net.liftweb.http.{S, MemoizeTransform, RequestVar, SHtml}
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.model.RulesDAO
import org.rules.lift.utils.{LiftRulesUtils, LiftUtils}
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
  private def modules = rulesDAO.getModules(RulesState.currentProjectName.get).getOrElse(Seq.empty)

  private def updateModule(name: String) = {
    val deselect = RulesState.currentModuleName match {
      case Some(module) => moduleGroup.deSelect(module)
      case _ => Noop
    }
    RulesState.setCurrentModuleName(name)

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
      rulesDAO.createModule(RulesState.currentProjectName.get, name)

      //Index.projectVar.set(Some(newProject))

      SetHtml("modules-list-container", renderModulesVar.is.get.applyAgain()) &
      Run("pack();")
    } else {
      Noop
    }
  }

  private def addModule() = {
    LiftUtils.bootboxPrompt("Module name", addModuleCall)
  }

  private def delModule(id: String) = {
    (for {
      newProject <- rulesDAO.delModule(RulesState.currentProjectName.get, id)
      result <- Full(
        RulesState.moduleDeleted(id) &
        SetHtml("modules-list-container", renderModulesVar.is.get.applyAgain()) &
        Run("pack();")
      )
    } yield result).getOrElse(Noop)
  }

  private def updateRules() : JsCmd = {
    LiftRulesUtils.beforeContentChange(
      SetHtml("content", RulesList.embed) &
      Run("$.ruleEditor = undefined;")
    )
  }


  private val renderModules = SHtml.memoize(
    "#modules-list *" #> modules.map(module =>
      ".select-module [onClick]" #> ajaxInvoke(() => updateModule(module.name)) &
      ".select-module *" #> module.name &
      ".list-rules [onClick]" #> ajaxInvoke(() => updateRules()) &
      ".modules-buttons [id]" #> modulesFinder.getDOMId(module.name) &
      ".modules-buttons [style+]" #> "display: none;" &
      ".del-module [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete module ${module.name}?",
          () => delModule(module.name))
    )
  )

  private object renderModulesVar extends RequestVar[Option[MemoizeTransform]](None)

  def render = {
    renderModulesVar.set(Some(renderModules))
    S.appendJs(Run("""$('[data-toggle="tooltip"]').tooltip();"""))

    "#modules-list-container *" #> renderModules &
    "#project-name *" #> RulesState.currentProjectName.get &
    "#add-module [onClick]" #> addModule()
  }
}
