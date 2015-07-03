package org.rules.lift.snippet

import net.liftweb.common.{Box, Full, Failure, Loggable}
import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Helpers._
import org.rules.lift._
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
  /*    LiftUtils.getOrElseError[XMLProject,JsCmd](
        rulesDAO.getProject(projectName),
        (project) => {
          RulesState.setCurrentProjectName(project.name)
          SetHtml("project-menu", <span class="lift:embed?what=/project-menu" />) &
          SetHtml("content", Text("")) &
          Run("pack();")
        },
        s"""Failed to load project "$projectName"""",
        Noop)
        */

/*
  private def addProject() = {
    LiftUtils.bootboxPrompt("Project name", addProject)
  }

  private def addProject(name: String) = {
    if (!name.isEmpty) {
      rulesDAO.createProject(name)

      SetHtml("projects-list-container", renderProjectsVar.is.get.applyAgain()) &
      Run("pack();")
    } else {
      Noop
    }
  }
*/

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

  def render() = {
    RulesState.resetCurrentRuleId
    renderRulesVar.set(Some(renderRules))

    "#rules-list-container *" #> renderRules &
    "#rules-list-title *" #> (RulesState.currentModuleName.get + " rules")
    /* &
    "#add-project [onClick]" #> addProject()*/
  }
}