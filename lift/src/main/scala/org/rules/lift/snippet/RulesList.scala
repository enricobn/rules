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

import scala.xml.{Text, Attribute, NodeSeq}

/**
 * Created by enrico on 6/25/15.
 */
object RulesList extends Loggable with RulesDAOProvider with LiftListView[XMLRule] {
  protected override val schemaResource = "/org/rules/lift/XMLRuleJSONSchema.json"

  protected override val template = "/rules-list"

  private def onEditorChange(state: State, json: JValue) = {
    val rule = fromJson(json)
    val renderedRule = renderRulesVar.is.get.applyAgain(RenderArgs(state, Seq(rule))) \ "_"
    Run(s"${state.itemFinder.find(rule.id).toJsCmd}.replaceWith(${encJs(renderedRule.toString)});") &
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

  private def updateRule(oldId: String, viewId: String, itemsGroup: JsGroup, rule: XMLRule): JsCmd = {
    JsRaw(
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
        if ('$oldId' != 'undefined') {
          ${itemsGroup.deSelect(oldId).toJsCmd}
        }
        ${itemsGroup.select(rule.id).toJsCmd}
      """.stripMargin
    )
  }

  private def renderRules(args: RenderArgs) : NodeSeq => NodeSeq = {
      ".list-elements *" #> args.items.map { rule =>
        ".select-item [onClick]" #> ajaxCall(JsRaw(s"""$$.liftViews["${args.state.viewId}"].activeId"""),
            (oldId) => updateRule(oldId, args.state.viewId, args.state.itemsGroup, rule)) &
          ".select-item [id]" #> args.state.itemFinder.getDOMId(rule.id) &
          ".select-item *" #> rule.name
      }
  }

  private object renderRulesVar extends RequestVar[Option[MemoizeTransformWithArg[RenderArgs]]](None)

  private def addRule(state: State) =
    LiftUtils.bootboxPrompt("Rule name", addRuleByName(state))

  private def addRuleByName(state: State)(name: String) = {
    if (!name.isEmpty) {
      val rule = rulesDAO.createRule(state.attributes("projectName"), state.attributes("moduleName"), name)
      val renderedRule = renderRulesVar.is.get.applyAgain(RenderArgs(state, Seq(rule))) \ "_"
      Run(
        s"""
          var view = $$.liftViews['${state.viewId}'];
          view.cache['${rule.id}'] = ${write(rule)};
          view.changed['${rule.id}'] = '${rule.id}';
          $$('#${state.viewId} .list-container').append(${encJs(renderedRule.toString)});
          ${state.itemFinder.find(rule.id).toJsCmd}.trigger('click');
        """
      )
    } else {
      Noop
    }
  }

  private def save(attributes: Map[String, String], viewId: String) =
    SHtml.jsonCall(JsRaw(s"editChanges('$viewId')"), new JsContext(Empty, Empty), (changedRules: JValue)=>{
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

      rulesDAO.updateRulesAndSave(attributes("projectName"), attributes("moduleName"), rules, deletedRules) match {
        case Full(result) => S.notice("Save succeeded")
        case Failure(msg, _, _) => S.error("Save error: " + msg)
        case _ => S.error("Save error")
      }
      Run(s"editAfterSave('$viewId');")
    })._2.toJsCmd

  def getItemFinder(attributes: Map[String, String]) : JsItemFinder =
    JQueryById(attributes("projectName") + "_" + attributes("moduleName"))

  def getItemsGroup(attributes: Map[String, String], itemFinder: JsItemFinder) =
    new JsSimpleGroup(itemFinder, CssClassApplier("active"))

  def getItems(attributes: Map[String,String]) =
    rulesDAO.getRules(attributes("projectName"), attributes("moduleName")).openOrThrowException("Error getting rules.")

  def render() = {
    val viewId = UUID.randomUUID().toString

    val attributes = S.attrs.map( attr =>
      attr match {
        case (Left(key), value) => (key -> value)
        case (Right((namespace, key)), value) => (namespace + ":" + key -> value)
      }
    ).toMap

    val itemFinder = getItemFinder(attributes)
    val itemsGroup = getItemsGroup(attributes, itemFinder)

    val state = State(attributes, viewId, itemFinder, itemsGroup)

    val items : Seq[XMLRule] = getItems(attributes)

    renderRulesVar.set(Some(memoizeWithArg(renderRules)))

    S.appendJs(
      Run(
        s"""$$('[data-toggle="tooltip"]').tooltip();
            editInit('$viewId', $$("#$viewId .detail-editor"), $schema, function(oldJson, newJson) {
              if (newJson.name != oldJson.name) {
                ${jsonCall(JsVar("newJson"), (json : JValue) => onEditorChange(state, json))._2.toJsCmd}
              }
            });
         """.stripMargin))

    ".list-main-container [id]" #> viewId &
    ".list-container *" #> renderRulesVar.is.get.apply(RenderArgs(state, items)) &
    ".add-item [onClick]" #> addRule(state) &
    ".del-item [onClick]" #> delItem("Are you sure to delete current rule?", state) &
    ".save-items [onclick]" #> save(attributes, viewId)
  }

}
