package org.rules.lift.snippet

import java.util.UUID

import net.liftweb.common._
import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.{JsVar, JsRaw}
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.utils.{LiftListView, MemoizeTransformWithArg, LiftUtils}
import LiftUtils._
import org.rules.rule.xml.XMLRule

import scala.xml.{Text, NodeSeq}

/**
 * Created by enrico on 6/25/15.
 */
object RulesList extends Loggable with RulesDAOProvider with LiftListView[XMLRule] {
  protected val schemaResource = "/org/rules/lift/XMLRuleJSONSchema.json"

  protected def getId(item: XMLRule) = item.id

  private def onEditorChange(state: State, json: JValue) = {
    val rule = fromJson(json)
    val id = getId(rule)
    val renderedRule = renderItemsVar.is.get.applyAgain(RenderArgs(state, Seq(rule))) \ "_"
    Run(s"${state.itemFinder.find(id).toJsCmd}.replaceWith(${encJs(renderedRule.toString)});") &
      state.itemsGroup.select(id)
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

  protected def renderItem(item: XMLRule) : NodeSeq =
    <div class="btn btn-primary rules-nav" style="float: left;">{item.name}</div>

  private def addRule(state: State) =
    LiftUtils.bootboxPrompt("Rule name", addRuleByName(state))

  private def addRuleByName(state: State)(name: String) = {
    if (!name.isEmpty) {
      val rule = rulesDAO.createRule(state.attributes("projectName"), state.attributes("moduleName"), name)
      val renderedRule = renderItemsVar.is.get.applyAgain(RenderArgs(state, Seq(rule))) \ "_"
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

  protected def save(state: State, changedItems: List[XMLRule], deletedItems: List[String]) : Box[Unit] =
    rulesDAO.updateRulesAndSave(state.attributes("projectName"), state.attributes("moduleName"), changedItems, deletedItems)

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

    renderItemsVar.set(Some(memoizeWithArg(renderItems)))

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
    ".list-container *" #> renderItemsVar.is.get.apply(RenderArgs(state, items)) &
    ".add-item [onClick]" #> addRule(state) &
    ".del-item [onClick]" #> delItem("Are you sure to delete current rule?", state) &
    ".save-items [onclick]" #> save(state)
  }

}
