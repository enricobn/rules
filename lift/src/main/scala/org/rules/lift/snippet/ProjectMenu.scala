package org.rules.lift.snippet

import java.util.UUID

import net.liftweb.common.Full
import net.liftweb.http.js.JE.{Str, AnonFunc, JsRaw}
import net.liftweb.http.js.{JsExp, JsCmd}
import net.liftweb.http._
import net.liftweb.http.js.JsExp._
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.JsonAST.{JObject, JNothing, JValue}
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.utils._
import net.liftweb.json.JsonDSL._

private case class Parameters(tabContentId: String, layoutContentId: String,
    layoutCenterContentId: String, listContainerId: String, projectName: String)

/**
 * Created by enrico on 6/22/15.
 * project-menu.html
 */
object ProjectMenu extends RulesDAOProvider with JQueryTabs {
  private val memo = JsMemo(this, 2)

  private def modules(projectName: String) = rulesDAO.getModules(projectName).openOrThrowException("Error getting modules")

  private def updateModule(parameters: Parameters, name: String) = {
    // lazy so it's not called if the tab already exists
    lazy val embedded = RulesListEditor.embed(Map("projectName" -> parameters.projectName, "moduleName" -> name))

    addTab(parameters.tabContentId, name, () => embedded.fragment,
      (nameExp, contentIdExp) =>
        Run(
          s"""
             |var close = true;
             |if (${RulesListEditor.hasUnsavedChanges(embedded.viewID).toJsCmd}) {
             |  close = confirm('There are pending changes. Close anyway?');
             |}
             |if (close) {
             |  ${RulesListEditor.clear(embedded.viewID).toJsCmd}
             |  ${memo.clear(parameters.projectName, name).toJsCmd}
             |}
             |return close;
           """.stripMargin
        ),
      () => memo.put(parameters.projectName, name)(embedded.viewID)
    )
  }

  def clearProject(projectName: String) = memo.clear(projectName)

  def hasUnsavedChanges(projectName: String) : JsExp =
    JsRaw(
      s"""
         |(function () {
         |  var m = ${memo.get(projectName).toJsCmd};
         |  if (typeof m == 'undefined') {
         |    return false;
         |  }
         |  var values = m.values();
         |  for (var i in values) {
         |    if (${RulesListEditor.hasUnsavedChanges(JsRaw("values[i]")).toJsCmd}) {
         |      return true;
         |    }
         |  }
         |  return false;
         |}())
      """.stripMargin)

  private def addModuleCall(parameters: Parameters)(name: String) = {
    if (!name.isEmpty) {
      rulesDAO.createModule(parameters.projectName, name).openOrThrowException("Error creating module")

      SetHtml(parameters.listContainerId, renderModulesVar.is.get.applyAgain(parameters)) &
      Run(
        s"""
           |var layout = $$('#${parameters.layoutContentId}').layout();
           |layout.resizeAll();
        """.stripMargin)
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
        Run(
          s"""
             |var layout = $$('#${parameters.layoutContentId}').layout();
             |layout.resizeAll();
          """.stripMargin)
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
           |$$('[data-toggle="tooltip"]').tooltip();
           |var layout = $$('#${parameters.layoutContentId}').layout({ applyDefaultStyles: false, west__size: "auto" });
        """.stripMargin) &
      createTabs(parameters.layoutCenterContentId, parameters.tabContentId, "heightStyle" -> "fill" ) &
      Run(
        s"""
           |layout.resizeAll();
           |$$(window).on('resize', function() {
           |  layout.resizeAll();
           |});
        """.stripMargin
      )
    )

    ".list-container *" #> renderModulesVar.is.get.apply(parameters) &
    ".list-container [id]" #> parameters.listContainerId &
    ".add-to-list [onClick]" #> addModule(parameters) &
    ".layout-content [id]" #> parameters.layoutContentId &
    ".layout-center-content [id]" #> parameters.layoutCenterContentId
  }
}
