package org.rules.lift.snippet

import java.util.UUID

import net.liftweb.common.Full
import net.liftweb.http.js.JE.{AnonFunc, JsRaw}
import net.liftweb.http.js.JsCmd
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.utils.{JQueryTabs, MemoizeTransformWithArg, LiftRulesUtils, LiftUtils}

private case class Parameters(tabContentId: String, layoutContentId: String,
    layoutCenterContentId: String, listContainerId: String, projectName: String)

/**
 * Created by enrico on 6/22/15.
 * project-menu.html
 */
class ProjectMenu extends RulesDAOProvider with JQueryTabs {
  private def modules(projectName: String) = rulesDAO.getModules(projectName).openOrThrowException("Error getting modules")

  private def updateModule(parameters: Parameters, name: String) = {
    // lazy so it's not called if the tab already exists
    lazy val embedded = RulesListEditor.embed(Map("projectName" -> parameters.projectName, "moduleName" -> name))

    addTab(parameters.tabContentId, name, () => embedded.fragment,
      (name, contentId) =>
        JsIf(RulesListEditor.hasUnsavedChanges(embedded.viewID), Run("return confirm('There are pending changes. Close anyway?');"),
          Run("return true;"))
    )
  }

  private def addModuleCall(parameters: Parameters)(name: String) = {
    if (!name.isEmpty) {
      rulesDAO.createModule(parameters.projectName, name).openOrThrowException("Error creating module")

      SetHtml(parameters.listContainerId, renderModulesVar.is.get.applyAgain(parameters)) &
        Run("pack();")
    } else {
      Noop
    }
  }

  private def addModule(parameters: Parameters) = {
    LiftUtils.bootboxPrompt("Module name", addModuleCall(parameters))
  }

  private def delModule(parameters: Parameters, name: String) = {
    (for {
      newProject <- rulesDAO.delModule(parameters.projectName, name)
      result <- Full(
        RulesState.moduleDeleted(name) &
        SetHtml(parameters.listContainerId, renderModulesVar.is.get.applyAgain(parameters)) &
        Run("pack();")
      )
    } yield result).getOrElse(Noop)
  }

  private def renderModules(parameters : Parameters) =
    ".list *" #> modules(parameters.projectName).map(module =>
      ".select-item [onClick]" #> ajaxInvoke(() => updateModule(parameters, module.name)) &
      ".select-item *" #> module.name &
      ".modules-buttons [style+]" #> "display: none;" &
      ".del-from-list [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete module ${module.name}?",
          () => delModule(parameters, module.name))
  )

  private object renderModulesVar extends RequestVar[Option[MemoizeTransformWithArg[Parameters]]](None)

  def render = {
    val parameters = Parameters(UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString,
      UUID.randomUUID().toString, S.attr("projectName").openOrThrowException("cannot find attribute projectName!!!"))

    renderModulesVar.set(Some(LiftUtils.memoizeWithArg(renderModules)))
    S.appendJs(
      Run(
        s"""
          $$('[data-toggle="tooltip"]').tooltip();
          var layout = $$('#${parameters.layoutContentId}').layout({ applyDefaultStyles: false, west__size: "auto" });
          layout.resizeAll();
        """.stripMargin) &
      createTabs(parameters.layoutCenterContentId, parameters.tabContentId)
    )

    ".list-container *" #> renderModulesVar.is.get.apply(parameters) &
    ".list-container [id]" #> parameters.listContainerId &
    ".add-to-list [onClick]" #> addModule(parameters) &
    ".layout-content [id]" #> parameters.layoutContentId &
    ".layout-center-content [id]" #> parameters.layoutCenterContentId
  }
}
