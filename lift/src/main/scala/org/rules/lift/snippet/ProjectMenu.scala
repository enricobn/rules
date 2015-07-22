package org.rules.lift.snippet

import java.util.UUID

import net.liftweb.common.Full
import net.liftweb.http.js.JsCmd
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.utils.{MemoizeTransformWithArg, LiftRulesUtils, LiftUtils}

private case class Parameters(layoutContentId: String, listContainerId: String, projectName: String)

/**
 * Created by enrico on 6/22/15.
 * project-menu.html
 */
class ProjectMenu extends RulesDAOProvider {
  private val modulesFinder = JQueryById("modules-buttons")
  private val moduleGroup : JQueryGroup = new JQueryGroup(modulesFinder, JQueryHide)


  //println("Constructor " + S.attr("projectName").openOrThrowException("cannot find attribute projectName!"))

  // TODO error check
  private def modules(projectName: String) = rulesDAO.getModules(projectName).getOrElse(Seq.empty)

  private def updateModule(parameters: Parameters, name: String) = {
/*    val deselect = RulesState.currentModuleName match {
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
    */
    println("update module " + name + " parameters " + parameters)
    Noop
  }

  private def addModuleCall(parameters: Parameters)(name: String) = {
    if (!name.isEmpty) {
      // TODO check errors
      rulesDAO.createModule(parameters.projectName, name)

      //Index.projectVar.set(Some(newProject))

      SetHtml(parameters.listContainerId, renderModulesVar.is.get.applyAgain(parameters)) &
        Run("pack();")
    } else {
      Noop
    }
  }

  private def addModule(parameters: Parameters) = {
    LiftUtils.bootboxPrompt("Module name", addModuleCall(parameters))
  }

  private def delModule(parameters: Parameters, id: String) = {
    (for {
      newProject <- rulesDAO.delModule(parameters.projectName, id)
      result <- Full(
        RulesState.moduleDeleted(id) &
        SetHtml(parameters.listContainerId, renderModulesVar.is.get.applyAgain(parameters)) &
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

  private def renderModules(parameters : Parameters) =
    ".list *" #> modules(parameters.projectName).map(module =>
      ".select-item [onClick]" #> ajaxInvoke(() => updateModule(parameters, module.name)) &
      ".select-item *" #> module.name &
      ".list-rules [onClick]" #> ajaxInvoke(() => updateRules()) &
      ".modules-buttons [id]" #> modulesFinder.getDOMId(module.name) &
      ".modules-buttons [style+]" #> "display: none;" &
      ".del-from-list [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete module ${module.name}?",
          () => delModule(parameters, module.name))
  )

  // argument is listContainerId,projectName
  private object renderModulesVar extends RequestVar[Option[MemoizeTransformWithArg[Parameters]]](None)

  def render = {
    val parameters = Parameters(UUID.randomUUID().toString, UUID.randomUUID().toString,
      S.attr("projectName").openOrThrowException("cannot find attribute projectName!!!"))

    renderModulesVar.set(Some(LiftUtils.memoizeWithArg(renderModules, parameters)))
    S.appendJs(Run(
      s"""
        $$('[data-toggle="tooltip"]').tooltip();
        var layout = $$('#${parameters.layoutContentId}').layout({ applyDefaultStyles: false, west__size: "auto" });
        layout.resizeAll();
      """.stripMargin))

    ".list-container *" #> renderModulesVar.is.get &
    ".list-container [id]" #> parameters.listContainerId &
    ".add-to-list [onClick]" #> addModule(parameters) &
    ".layout-content [id]" #> parameters.layoutContentId
  }
}
