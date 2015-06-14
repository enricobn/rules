package org.rules.lift.snippet

/**
 * Created by enrico on 6/5/15.
 */
import net.liftweb._
import http._
import net.liftweb.common.{Full, Empty}
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.JsonAST._
import org.rules.lift.snippet.Ajax._
import org.rules.rule.xml.XMLRule
import scala.util.parsing.json.JSONObject
import scala.xml.{Text, NodeSeq}
import net.liftweb.util.BindHelpers._
import net.liftweb.json.Xml.{toJson, toXml}
import net.liftweb.json.{DefaultFormats, pretty, render}
import net.liftweb.json.JsonDSL._

/**
 * A RequestVar-based snippet
 */
object EditRules {
  /*
  private object ruleVar extends RequestVar[Option[XMLRule]](None)
  private object requiresVar extends RequestVar("")
  private object providesVar extends RequestVar("")
  private object runVar extends RequestVar("")
*/
  def embed() = {
    val is = getClass().getResourceAsStream("/org/rules/rule/xml/XMLRuleJSONSchema.json")
    val schema = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    <span class="lift:embed?what=/editrules" /> ++
    Script(OnLoad( Run(
      s"""
        if (typeof $$.jsonEditor != 'undefined') {
          $$.jsonEditor.destroy();
        }

        if (typeof $$.jsonFromServer == 'undefined') {
          // it's the callback of getting an item from the server, v is the Json value
          $$.jsonFromServer = function(v) {
            console.log('got json from server');
            $$.jsonValues[v.name] = v;
            $$.jsonEditor.setValue(v);
            $$.jsonActiveId =  v.name;
            $$("#detail").show();
          };

          // it's the callback of the error getting an item from the server
          $$.jsonFromServerFailure = function() {
            console.log('Error getting json from server');
            // TODO do it better
            alert('Error getting json from server.');
          };
        }

        JSONEditor.defaults.options.theme = 'bootstrap3';
        JSONEditor.defaults.iconlib = 'bootstrap3';
        JSONEditor.defaults.options.disable_edit_json = true;
        JSONEditor.defaults.options.disable_properties = true;

        $$("#detail").empty();

        $$("#detail").hide();

        $$.jsonEditor = new JSONEditor(document.getElementById("detail"), $schema);

        // on change values in the editor, I update the cache
        $$.jsonEditor.on('change', function() {
          if (typeof $$.jsonActiveId != 'undefined') {
            $$.jsonValues[$$.jsonActiveId] = $$.jsonEditor.getValue();
          }
        });

        // the cache of items, key=id value=json
        $$.jsonValues = new Object();

        // the id of the active item
        $$.jsonActiveId = undefined;
      """
    )))
  }

  /*
  private def jsonEditor(rule: XMLRule) : JsCmd = {
    lazy val jsonRule = json.compact(json.render(ruleToJson(rule)))

    Run(
      s"""
      $$.jsonEditor.setValue($jsonRule);
      $$.getJson('id');
      """
    )
  }
*/
  def listRules (xhtml: NodeSeq): NodeSeq = {
    <h2 style="margin-left: 10px;">Rules</h2> ++
      Index.moduleVar.get.get.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ render(rule)}
  }

  /*
  def processSave() = {
    println("Save requires=" + requiresVar + " provides=" + providesVar + " run=" + runVar)
  }
*/
  /*
  def submitRule(xhtml: NodeSeq): NodeSeq = {
    ruleVar.is match {
      case Some (rule) =>
        requiresVar.set(rule.requires.toString())
        providesVar.set(rule.provides.toString())
        runVar.set(rule.run.toString)

        val cssTransform = ".requires" #> SHtml.text(requiresVar.is, requiresVar(_)) &
          ".provides" #> SHtml.text(providesVar.is, providesVar(_)) &
          ".runScript" #> SHtml.text(runVar.is, runVar(_))
        cssTransform(xhtml) ++ SHtml.hidden(processSave)
      case _ => Text("Cannot find rule in request")
    }
  }
*/

  private def getRule(id: String) = Index.moduleVar.get.get.rules.find(_.name == id).get

  def ruleOnClick(node: NodeSeq) : NodeSeq = {
    val name = node.head.\@("rule-name")

    val cssTransform = ".rule [onclick]" #>
      Script(JsRaw(
        s"""
          if (typeof $$.jsonValues['$name'] != 'undefined') {
            $$.jsonEditor.setValue($$.jsonValues['$name']);
            $$.jsonActiveId = '$name';
          } else {
        """ +
        SHtml.jsonCall(name, new JsonContext(Full("$.jsonFromServer"), Full("$.jsonFromServerFailure")), (a:Any)=>{
          ruleToJson(getRule(name))
        })._2.toJsCmd
        + "}"
      ))


//    jsonEditor(getRule(name))
/*      SHtml.ajaxInvoke(() => {
        val name = node.head.\@("rule-name")
        ruleVar.set(Ajax.moduleVar.get.get.rules.find(_.name == name))
        println("***** click on " + name)
        SetHtml("detail", <span class="lift:embed?what=/ruledetail" />)
      })
      */
    cssTransform(node)
  }

  def jsonToRule(json: JValue) : XMLRule = {
    implicit val formats = DefaultFormats
    json.extract[XMLRule]
  }

  def ruleToJson(rule: XMLRule) : JValue = {
    ("name" -> rule.name) ~
    ("tags" -> rule.tags) ~
    ("requires" -> rule.requires.map{ r =>
        (("token" -> r.token) ~
         ("tags" -> r.tags.toString))
      }
    ) ~
    ("provides" -> rule.provides.map{ p =>
        (("token" -> p.token) ~
         ("value" -> p.value))
      }
    ) ~
    ("run" -> rule.run.getOrElse(""))
  }

  private def render(rule: XMLRule) : NodeSeq = {
/*    println(rule.toXML())

    val json = ruleToJson(rule)

    println(json)

    println(pretty(render(json)))

    println(jsonToRule(json).toXML())
    */

    <div data-lift="EditRules.ruleOnClick" rule-name={rule.name}>
      <span class="btn btn-default rule" style="margin-top: 5px; margin-left: 10px;">{rule.name}</span>
    </div>
  }
}