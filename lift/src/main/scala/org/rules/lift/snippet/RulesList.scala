package org.rules.lift.snippet

import net.liftweb.common._
import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Helpers._
import org.rules.lift._
import LiftUtils._
import org.rules.rule.xml.XMLRule

import scala.xml.NodeSeq

/**
 * Created by enrico on 6/25/15.
 */
object RulesList extends Loggable with RulesDAOProvider {
  private val rulesFinder = JQueryById("rules-buttons")
  private val ruleGroup: JQueryGroup = new JQueryGroup(rulesFinder, JQueryActivate)

  def ruleToJson(rule: XMLRule): JValue = {
    ("id" -> rule.id) ~
      ("name" -> rule.name) ~
      ("tags" -> rule.tags) ~
      ("requires" -> rule.requires.map { r =>
        (("token" -> r.token) ~
          ("tags" -> r.tags.toString))
      }) ~
      ("provides" -> rule.provides.map { p =>
        (("token" -> p.token) ~
          ("value" -> p.value))
      }) ~
      ("run" -> rule.run)
  }

  def jsonToRule(json: JValue) : XMLRule = {
    implicit val formats = DefaultFormats
    json.extract[XMLRule]
  }

  private def updateRule(rule: XMLRule): JsCmd = {
    val result = JsRaw(
      s"""
            if (typeof $$.changedRules.jsonValues['${rule.id}'] != 'undefined') {
              $$.jsonEditor.setValue($$.changedRules.jsonValues['${rule.id}']);
              $$.jsonActiveId = '${rule.id}';
            } else {
          """ +
        SHtml.jsonCall(rule.id, new JsonContext(Full("$.jsonFromServer"),
          Full("$.jsonFromServerFailure")), (a: Any) => {
          ruleToJson(rule)
        }
        )._2.toJsCmd +
        "}\npack();"
    ) &
    /*SetHtml("rules-list-container", renderRulesVar.is.get.applyAgain()) &*/
    ruleGroup.select(rule.id) &
    RulesState.currentRuleId.map(ruleGroup.deSelect(_)).getOrElse(Noop)

    RulesState.setCurrentRuleId(rule.id)

    result
  }

  private def renderRules(rules: Seq[XMLRule]) : NodeSeq => NodeSeq = {
    // TODO error check
      "#rules-list-elements *" #> rules.map { rule =>
        ".select-rule [onClick]" #> ajaxInvoke(() => updateRule(rule)) &
          ".select-rule [id]" #> rulesFinder.getJQueryId(rule.id) &
          ".select-rule *" #> rule.name /*&
        ".del-project [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete project ${project.name}?",
          () => delProject(project.name))*/
      }
  }

  private object renderRulesVar extends RequestVar[Option[MemoizeTransformWithArg[Seq[XMLRule]]]](None)

  private def addRule = LiftUtils.bootboxPrompt("Rule name", addRuleByName)

  private def addRuleByName(name: String) = {
    if (!name.isEmpty) {
      /*
      Run(
        s"""
          var rule = new Object();
          rule.name = '$name';
          $$.changedRules.jsonValues[id] = rule;
        """
      )
      */
      val rule = rulesDAO.createRule(RulesState.currentProjectName.get, RulesState.currentModuleName.get, name)
      val renderedRules = renderRulesVar.is.get.applyAgain(Seq(rule.get))
      val renderedRule = (renderedRules \\ "_").filter{node =>(node \ "@id").text == rulesFinder.getJQueryId(rule.get.id)}
//      println("**** RENDERRULE ****" + (renderedRule \\ "_"))
      println("**** RENDERRULE ****" + renderedRule)

      implicit val rules : Seq[XMLRule] = rulesDAO.getRules(RulesState.currentProjectName.get, RulesState.currentModuleName.get).get
      LiftUtils.getOrElseError[XMLRule, JsCmd](
        rule,
        (r) => SetHtml("rules-list-container", renderRulesVar.is.get.applyAgain(rules)) &
          updateRule(r),
        s"""Cannot create rule "$name"""",
        Noop
      )
    } else {
      Noop
    }
  }

  private def delRule =
    Run(
        s"""
           if (typeof $$.jsonActiveId != 'undefined') {
              $$('#${rulesFinder.idPrefix}' + '-' + $$.jsonActiveId).hide();
              $$.changedRules.deleted.push($$.jsonActiveId);
              $$("#detail-editor").hide();
              $$.jsonActiveId = undefined;
            }
        """)

  private def save =
    SHtml.jsonCall(JsRaw("$.changedRules"), new JsContext(Empty, Empty), (changedRules: JValue)=>{
      //            println(values)
      val rules = changedRules \ "jsonValues" match {
        case JObject(x :: xs) =>
          val l = x :: xs
          l.foldLeft(List.empty[XMLRule]){ (actual, field) => actual.:+(jsonToRule(field.value))}
        case _ => List.empty[XMLRule]
      }

      val deletedRules = changedRules \ "deleted" match {
        case JArray(l: List[JString]) =>
          l.foldLeft(List.empty[String]){ (actual, field) => actual.:+(field.s)}
        case _ => List.empty[String]
      }

      rulesDAO.updateRulesAndSave(RulesState.currentProjectName.get, RulesState.currentModuleName.get, rules, deletedRules) match {
        case Full(result) => S.notice("Save succeeded")
        case Failure(msg, _, _) => S.error("Save error: " + msg)
        case _ => S.error("Save error")
      }
      Noop
    })._2.toJsCmd

  def render() = {
    RulesState.resetCurrentRuleId
    val rules : Seq[XMLRule] =
      rulesDAO.getRules(RulesState.currentProjectName.get, RulesState.currentModuleName.get).get

    renderRulesVar.set(Some(memoizeWithArg(renderRules, rules)))

    "#rules-list-container *" #> renderRulesVar.is.get &
    "#rules-list-title *" #> (RulesState.currentModuleName.get + " rules") &
    "#add-rule [onClick]" #> addRule &
    "#del-rule [onClick]" #> delRule &
    "#rules-save [onclick]" #> save
  }

}
