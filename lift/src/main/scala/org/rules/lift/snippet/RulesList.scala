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
  private val rulesFinder = JQueryById("rules-buttons")
  private val ruleGroup: JQueryGroup = new JQueryGroup(rulesFinder, JQueryActivate)

  def embed(projectName: String, moduleName: String) = {
    val viewId = UUID.randomUUID().toString
    val is = getClass().getResourceAsStream("/org/rules/lift/XMLRuleJSONSchema.json")
    val schema = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    <lift:embed what="/rules-list" viewId={viewId} projectName={projectName} moduleName={moduleName}></lift:embed> ++
      Script(OnLoad( Run(
        s"""
        editInit('$viewId', $$("#detail-editor"), $schema, function(oldJson, newJson) {
          if (newJson.name != oldJson.name) {
            ${jsonCall(JsVar("newJson"), (json : JValue) => onEditorChange(viewId, json))._2.toJsCmd}
          }
        });
      """
      )))
  }

  private def onEditorChange(viewId: String, json: JValue) = {
    val rule = fromJson(json)
    val renderedRule = renderRulesVar.is.get.applyAgain((viewId, Seq(rule))) \ "_"
    Run(s"${rulesFinder.find(rule.id).toJsCmd}.replaceWith('$renderedRule');") &
      ruleGroup.select(rule.id)
  }

  def toJson(rule: XMLRule): JValue = {
    ("id" -> rule.id) ~
    ("name" -> rule.name) ~
    ("tags" -> rule.tags) ~
    ("requires" -> rule.requires.map { r => ("token" -> r.token) ~ ("tags" -> r.tags.toString) }) ~
    ("provides" -> rule.provides.map { p => ("token" -> p.token) ~ ("value" -> p.value) }) ~
    ("run" -> rule.run)
  }

  def fromJson(json: JValue) : XMLRule = {
    implicit val formats = DefaultFormats
    json.extract[XMLRule]
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
    ) &
    RulesState.currentRuleId.map(ruleGroup.deSelect(_)).getOrElse(Noop) &
      ruleGroup.select(rule.id)

    RulesState.setCurrentRuleId(rule.id)

    result
  }

  private def renderRules(rules : (String, Seq[XMLRule])) : NodeSeq => NodeSeq = {
      "#rules-list-elements *" #> rules._2.map { rule =>
        ".select-rule [onClick]" #> ajaxInvoke(() => updateRule(rules._1, rule)) &
          ".select-rule [id]" #> rulesFinder.getDOMId(rule.id) &
          ".select-rule *" #> rule.name
      }
  }

  private object renderRulesVar extends RequestVar[Option[MemoizeTransformWithArg[(String,Seq[XMLRule])]]](None)

  private def addRule(projectName: String, moduleName: String, viewId: String) =
    LiftUtils.bootboxPrompt("Rule name", addRuleByName(projectName, moduleName, viewId))

  private def addRuleByName(projectName: String, moduleName: String, viewId: String)(name: String) = {
    if (!name.isEmpty) {
      val rule = rulesDAO.createRule(projectName, moduleName, name)
      val renderedRule = renderRulesVar.is.get.applyAgain((viewId, Seq(rule))) \ "_"
      Run(
        s"""
          var view = $$.liftViews['$viewId'];
          view.cache['${rule.id}'] = ${write(rule)};
          view.changed['${rule.id}'] = '${rule.id}';
          $$('#rules-list-container').append('$renderedRule');
          ${rulesFinder.find(rule.id).toJsCmd}.trigger('click');
        """
      )
    } else {
      Noop
    }
  }

  private def delRule(viewId: String) =
    Run(
        s"""
           var view = $$.liftViews['$viewId'];
           if (typeof view.activeId != 'undefined') {
              view.editingActive = false;
              ${rulesFinder.find(JsRaw("view.activeId")).toJsCmd}.hide();
              view.deleted.push(view.activeId);
              $$("#detail-editor").hide();
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
    RulesState.resetCurrentRuleId
    val rules : Seq[XMLRule] =
      rulesDAO.getRules(projectName, moduleName).get

    renderRulesVar.set(Some(memoizeWithArg(renderRules, (viewId, rules))))

    S.appendJs(Run("""$('[data-toggle="tooltip"]').tooltip();"""))

    "#rules-list-container *" #> renderRulesVar.is.get &
    "#add-rule [onClick]" #> addRule(projectName, moduleName, viewId) &
    "#del-rule [onClick]" #> delRule(viewId) &
    "#rules-save [onclick]" #> save(projectName, moduleName, viewId)
  }

}
