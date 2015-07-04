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
import org.rules.lift.snippet.ProjectsList._
import org.rules.rule.xml.{XMLRule, XMLProject}

import scala.xml.Text

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
            if (typeof $$.jsonValues['${rule.id}'] != 'undefined') {
              $$.jsonEditor.setValue($$.jsonValues['${rule.id}']);
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

  private val renderRules = SHtml.memoize {
    // TODO error check
    "#rules-list-elements *" #> rulesDAO.getRules(RulesState.currentProjectName.get, RulesState.currentModuleName.get).get.map{ rule =>
      ".select-rule [onClick]" #> ajaxInvoke(() => updateRule(rule)) &
        ".select-rule [id]" #> rulesFinder.getJQueryId(rule.id) &
        ".select-rule *" #> rule.name /*&
        ".del-project [onClick]" #> LiftUtils.bootboxConfirm(s"Are you sure to delete project ${project.name}?",
          () => delProject(project.name))*/
    }
  }

  private object renderRulesVar extends RequestVar[Option[MemoizeTransform]](None)

  private def addRule = LiftUtils.bootboxPrompt("Rule name", addRuleByName)

  private def addRuleByName(name: String) = {
    if (!name.isEmpty) {
      val rule = rulesDAO.createRule(RulesState.currentProjectName.get, RulesState.currentModuleName.get, name)
      LiftUtils.getOrElseError[XMLRule, JsCmd](
        rule,
        (r) => SetHtml("rules-list-container", renderRulesVar.is.get.applyAgain()) &
          updateRule(r),
        s"""Cannot create rule "$name"""",
        Noop
      )
    } else {
      Noop
    }
  }

  private def save =
    SHtml.jsonCall(JsRaw("$.jsonValues"), new JsContext(Empty, Empty), (values: JValue)=>{
      //            println(values)
      val rules = values match {
        case JObject(x :: xs) =>
          val l = x :: xs
          l.foldLeft(List.empty[XMLRule]){ (actual, field) => actual.:+(jsonToRule(field.value))}
        //l.foreach{ field => println("##### Saved " + jsonToRule(field))}
        case _ => List.empty[XMLRule]
      }

      rulesDAO.updateRuleAndSave(RulesState.currentProjectName.get, RulesState.currentModuleName.get, rules) match {
        case Full(result) => S.notice("Save succeeded")
        case Failure(msg, _, _) => S.error("Save error: " + msg)
        case _ => S.error("Save error")
      }
      Noop
    })._2.toJsCmd

  def render() = {
    RulesState.resetCurrentRuleId
    renderRulesVar.set(Some(renderRules))

    "#rules-list-container *" #> renderRules &
    "#rules-list-title *" #> (RulesState.currentModuleName.get + " rules") &
    "#add-rule [onClick]" #> addRule &
    "#rules-save [onclick]" #> save
  }
}
