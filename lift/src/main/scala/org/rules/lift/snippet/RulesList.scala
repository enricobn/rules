package org.rules.lift.snippet

import java.util.UUID

import net.liftweb.common._
import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.{JsVar, JsRaw}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.{Serialization, DefaultFormats}
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.sitemap.*
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.utils.{LiftListView, MemoizeTransformWithArg, LiftUtils}
import LiftUtils._
import org.rules.rule.xml.XMLRule

import scala.xml.NodeSeq

/**
 * Created by enrico on 6/25/15.
 */


object RulesList extends Loggable with RulesDAOProvider with LiftListView[XMLRule] {

  private case class RulesListState(projectName: String, moduleName: String, viewId: String, itemsFinder: JQueryById,
                            itemsGroup: JsGroup)

  private case class RenderArgs(itemsFinder: JQueryById, viewID: String, items: Seq[XMLRule])

  def embed(projectName: String, moduleName: String) = {
    val viewId = UUID.randomUUID().toString

    <lift:embed what="/rules-list" viewId={viewId} projectName={projectName} moduleName={moduleName}></lift:embed>
  }

  private def onEditorChange(state: RulesListState, json: JValue) = {
    val rule = fromJson(json)
    val renderedRule = renderRulesVar.is.get.applyAgain(RenderArgs(state.itemsFinder, state.viewId, Seq(rule))) \ "_"
    Run(s"${state.itemsFinder.find(rule.id).toJsCmd}.replaceWith('$renderedRule');") &
      state.itemsGroup.select(rule.id)
  }

  def toJson(item: XMLRule): JValue = {
    ("id" -> item.id) ~
    ("name" -> item.name) ~
    ("tags" -> item.tags) ~
    ("requires" -> item.requires.map { r => ("token" -> r.token) ~ ("tags" -> r.tags.toString) }) ~
    ("provides" -> item.provides.map { p => ("token" -> p.token) ~ ("value" -> p.value) }) ~
    ("run" -> item.run)
  }

  def fromJson(jsonItem: JValue) : XMLRule = {
    implicit val formats = DefaultFormats
    jsonItem.extract[XMLRule]
  }

  private def updateRule(viewId: String, rule: XMLRule): JsCmd = {
    val result = JsRaw(
      s"""
        var view = $$.liftViews['$viewId'];
        view.jsonEditor.disable();
        view.jsonEditor.off('change', view.changeListener);
        view.editingActive = false;
        if (typeof view.cache['${rule.id}'] != 'undefined') {
          view.updateEditor(view.cache['${rule.id}']);
        } else {
          view.updateEditor(${write(rule)});
        }
      """
    ) /*&
    RulesState.currentRuleId.map(ruleGroup.deSelect(_)).getOrElse(Noop) &
      ruleGroup.select(rule.id)

    RulesState.setCurrentRuleId(rule.id)*/

    result
  }

  private def renderRules(arg: RenderArgs) : NodeSeq => NodeSeq = {
      ".list-elements *" #> arg.items.map { rule =>
        ".select-item [onClick]" #> ajaxInvoke(() => updateRule(arg.viewID, rule)) &
          ".select-item [id]" #> arg.itemsFinder.getDOMId(rule.id) &
          ".select-item *" #> rule.name
      }
  }

  private object renderRulesVar extends RequestVar[Option[MemoizeTransformWithArg[RenderArgs]]](None)

  private def addRule(state: RulesListState) =
    LiftUtils.bootboxPrompt("Rule name", addRuleByName(state))

  private def addRuleByName(state: RulesListState)(name: String) = {
    if (!name.isEmpty) {
      val rule = rulesDAO.createRule(state.projectName, state.moduleName, name)
      val renderedRule = renderRulesVar.is.get.applyAgain(RenderArgs(state.itemsFinder, state.viewId, Seq(rule))) \ "_"
      Run(
        s"""
          var view = $$.liftViews['${state.viewId}'];
          view.cache['${rule.id}'] = ${write(rule)};
          view.changed['${rule.id}'] = '${rule.id}';
          $$('#${state.viewId} .list-container').append('$renderedRule');
          ${state.itemsFinder.find(rule.id).toJsCmd}.trigger('click');
        """
      )
    } else {
      Noop
    }
  }

  private def delRule(state: RulesListState) =
    Run(
        s"""
           var view = $$.liftViews['${state.viewId}'];
           if (typeof view.activeId != 'undefined') {
              view.editingActive = false;
              ${state.itemsFinder.find(JsRaw("view.activeId")).toJsCmd}.hide();
              view.deleted.push(view.activeId);
              $$("#${state.viewId} .detail-editor").hide();
              view.activeId = undefined;
            }
        """)

  private def save(projectName: String, moduleName: String, viewId: String) =
    SHtml.jsonCall(JsRaw(s"editChanges('$viewId')"), new JsContext(Empty, Empty), (changedRules: JValue)=>{
      println(changedRules)
      val rules = changedRules \ "changed" match {
        case JObject(x :: xs) =>
          val l = x :: xs
          l.foldLeft(List.empty[XMLRule]){ (actual, field) => actual.:+(fromJson(field.value))}
        case _ => List.empty[XMLRule]
      }

      val deletedRules = changedRules \ "deleted" match {
        case JArray(l: List[JString]) =>
          l.foldLeft(List.empty[String]){ (actual, field) => actual.:+(field.s)}
        case _ => List.empty[String]
      }

      rulesDAO.updateRulesAndSave(projectName, moduleName, rules, deletedRules) match {
        case Full(result) => S.notice("Save succeeded")
        case Failure(msg, _, _) => S.error("Save error: " + msg)
        case _ => S.error("Save error")
      }
      Run(s"editAfterSave('$viewId');")
    })._2.toJsCmd

  def render() = {
    val viewId : String = S.attr("viewId").openOrThrowException("cannot find attribute viewId!")
    val projectName : String = S.attr("projectName").openOrThrowException("cannot find attribute projectName!")
    val moduleName : String = S.attr("moduleName").openOrThrowException("cannot find attribute moduleName!")
    val itemsFinder = JQueryById(projectName + "_" + moduleName)
    val itemsGroup: JsSimpleGroup = new JsSimpleGroup(itemsFinder, CssClassApplier("active"))

    val state = RulesListState(projectName, moduleName, viewId, itemsFinder, itemsGroup)

    RulesState.resetCurrentRuleId
    val rules : Seq[XMLRule] =
      rulesDAO.getRules(projectName, moduleName).openOrThrowException("Error getting rules.")

    renderRulesVar.set(Some(memoizeWithArg(renderRules, RenderArgs(itemsFinder, viewId, rules))))

    val is = getClass().getResourceAsStream("/org/rules/lift/XMLRuleJSONSchema.json")
    val schema = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    S.appendJs(
      Run(
        s"""$$('[data-toggle="tooltip"]').tooltip();
            editInit('$viewId', $$("#$viewId .detail-editor"), $schema, function(oldJson, newJson) {
              if (newJson.name != oldJson.name) {
                ${jsonCall(JsVar("newJson"), (json : JValue) => onEditorChange(state, json))._2.toJsCmd}
              }
            });
         """.stripMargin))

    ".list-container *" #> renderRulesVar.is.get &
    ".list-main-container [id]" #> viewId &
    ".add-item [onClick]" #> addRule(state) &
    ".del-item [onClick]" #> delRule(state) &
    ".save-items [onclick]" #> save(projectName, moduleName, viewId)
  }

}
