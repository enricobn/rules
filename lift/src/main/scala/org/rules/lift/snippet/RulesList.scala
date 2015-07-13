package org.rules.lift.snippet

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
import org.rules.lift.utils.{LiftList, MemoizeTransformWithArg, LiftUtils}
import LiftUtils._
import org.rules.rule.xml.XMLRule

import scala.xml.NodeSeq

/**
 * Created by enrico on 6/25/15.
 */
object RulesList extends Loggable with RulesDAOProvider with LiftList[XMLRule] {
  private val rulesFinder = JQueryById("rules-buttons")
  private val ruleGroup: JQueryGroup = new JQueryGroup(rulesFinder, JQueryActivate)

  def embed() = {
    val is = getClass().getResourceAsStream("/org/rules/lift/XMLRuleJSONSchema.json")
    val schema = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    <div class="lift:embed?what=/rules-list"></div> ++
      Script(OnLoad( Run(
        s"""
        if (typeof $$.jsonEditor != 'undefined') {
          $$.jsonEditor.destroy();
        }

        JSONEditor.defaults.options.theme = 'bootstrap3';
        JSONEditor.defaults.iconlib = 'bootstrap3';
        JSONEditor.defaults.options.disable_edit_json = true;
        JSONEditor.defaults.options.disable_properties = true;
        JSONEditor.defaults.options.disable_collapse = true;

        $$("#detail-editor").empty();

        $$("#detail-editor").hide();

        $$.jsonEditor = new JSONEditor(document.getElementById("detail-editor"), $schema);

        // to hide the title of the editor
        $$( "span:contains('hide-me')" ).parent().hide();

        $$.changedRules = new Object();
        editInit($$.changedRules);

        pack();
      """
      )))
  }

  def toJson(rule: XMLRule): JValue = {
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

  def fromJson(json: JValue) : XMLRule = {
    implicit val formats = DefaultFormats
    json.extract[XMLRule]
  }

  private def updateRule(rule: XMLRule): JsCmd = {
    val result = JsRaw(
      s"""
        $$.jsonEditor.disable();
        $$.jsonEditor.off('change', $$.changedRules.changeListener);
        $$.changedRules.editingActive = false;
        if (typeof $$.changedRules.cache['${rule.id}'] != 'undefined') {
          $$.changedRules.updateEditor($$.changedRules.cache['${rule.id}']);
        } else {
          $$.changedRules.updateEditor(${write(rule)});
        }
      """
    ) &
    RulesState.currentRuleId.map(ruleGroup.deSelect(_)).getOrElse(Noop) &
      ruleGroup.select(rule.id)

    RulesState.setCurrentRuleId(rule.id)

    result
  }

  private def renderRules(rules: Seq[XMLRule]) : NodeSeq => NodeSeq = {
      "#rules-list-elements *" #> rules.map { rule =>
        ".select-rule [onClick]" #> ajaxInvoke(() => updateRule(rule)) &
          ".select-rule [id]" #> rulesFinder.getDOMId(rule.id) &
          ".select-rule *" #> rule.name
      }
  }

  private object renderRulesVar extends RequestVar[Option[MemoizeTransformWithArg[Seq[XMLRule]]]](None)

  private def addRule = LiftUtils.bootboxPrompt("Rule name", addRuleByName)

  private def addRuleByName(name: String) = {
    if (!name.isEmpty) {
      val rule = rulesDAO.createRule(RulesState.currentProjectName.get, RulesState.currentModuleName.get, name)
      val renderedRule = renderRulesVar.is.get.applyAgain(Seq(rule)) \ "_"
      Run(
        s"""
          $$.changedRules.cache['${rule.id}'] = ${write(rule)};
          $$.changedRules.changed['${rule.id}'] = '${rule.id}';
          $$('#rules-list-container').append('$renderedRule');
          ${rulesFinder.find(rule.id).toJsCmd}.trigger('click');
        """
      )
    } else {
      Noop
    }
  }

  private def delRule =
    Run(
        s"""
           if (typeof $$.changedRules.activeId != 'undefined') {
              $$.changedRules.editingActive = false;
              ${rulesFinder.find(JsRaw("$.changedRules.activeId")).toJsCmd}.hide();
              $$.changedRules.deleted.push($$.changedRules.activeId);
              $$("#detail-editor").hide();
              $$.changedRules.activeId = undefined;
            }
        """)

  private def save =
    SHtml.jsonCall(JsRaw("editChanges($.changedRules)"), new JsContext(Empty, Empty), (changedRules: JValue)=>{
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

      rulesDAO.updateRulesAndSave(RulesState.currentProjectName.get, RulesState.currentModuleName.get, rules, deletedRules) match {
        case Full(result) => S.notice("Save succeeded")
        case Failure(msg, _, _) => S.error("Save error: " + msg)
        case _ => S.error("Save error")
      }
      Run("editAfterSave($.changedRules);")
    })._2.toJsCmd

  def render() = {
    RulesState.resetCurrentRuleId
    val rules : Seq[XMLRule] =
      rulesDAO.getRules(RulesState.currentProjectName.get, RulesState.currentModuleName.get).get

    renderRulesVar.set(Some(memoizeWithArg(renderRules, rules)))

    S.appendJs(Run("""$('[data-toggle="tooltip"]').tooltip();"""))

    "#rules-list-container *" #> renderRulesVar.is.get &
    "#rules-list-title *" #> (RulesState.currentModuleName.get + " rules") &
    "#add-rule [onClick]" #> addRule &
    "#del-rule [onClick]" #> delRule &
    "#rules-save [onclick]" #> save
  }

}
