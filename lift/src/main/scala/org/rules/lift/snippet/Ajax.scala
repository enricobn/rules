package org.rules.lift.snippet

import java.io.File
import java.nio.file.Files

import net.liftweb.json
import net.liftweb.json.JsonAST.{JArray, JValue}
import net.liftweb.json.JsonDSL
import org.rules.rule.xml.{XMLRule, XMLModule, XMLProject}
import net.liftweb.http.{SHtml, SessionVar}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.{SetHtml, CmdPair, Run}
import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml._
import scala.xml.{NodeSeq, Text}
import net.liftweb.json.JsonDSL._
// The next two imports are used to get some implicit conversions
// in scope.
import _root_.net.liftweb.http.js.JsCmds._

/**
 * Created by enrico on 6/7/15.
 */
object Ajax {


/*
  private def rulesToJson(rules: Seq[XMLRule]) : JValue = {
    JArray(rules.map(EditRules.ruleToJson(_)).toList)
  }
  */
/*
  private def jsonEditor(module: XMLModule) : JsCmd = {
    val is = getClass().getResourceAsStream("/org/rules/rule/xml/XMLRuleJSONSchema.json")
    val schema = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    val script =
      """
          JSONEditor.defaults.options.theme = 'bootstrap3';

          // Initialize the editor
          var editor = new JSONEditor(document.getElementById("content"),
      """ + schema +
      """);

          // Set the value
          editor.setValue(
      """ + json.compact(json.render(rulesToJson(module.rules))) +
      ");\n"
    //println(script)
    Run(script)
  }
*/


  private def ruleSeq(rule: XMLRule) : NodeSeq = {
    <h3>{rule.name}</h3> ++
    Text("requires: ") ++ Text(rule.requires.toString()) ++
    <br></br> ++
    Text("provides: ") ++ Text(rule.provides.toString()) ++
    <br></br> ++
    Text("runScript: ") ++ Text(rule.run.toString)
  }

  def addRule(xhtml : NodeSeq) : NodeSeq = {
    var requires = findNode(<entry:requires></entry:requires>, xhtml).head.\@("value")
    var provides = findNode(<entry:provides></entry:provides>, xhtml).head.\@("value")
    var runScript = findNode(<entry:runScript></entry:runScript>, xhtml).head.\@("value")

    def processEntryAdd() = {
      println(requires + "," + provides + "," + runScript)
    }

    bind("entry", xhtml,
      "requires" -> SHtml.text(requires, requires = _),
      "provides" -> SHtml.text(provides, provides = _),
      "runScript" -> (SHtml.text(runScript, runScript = _) ++ SHtml.hidden(processEntryAdd))
    )

  }

  private def ruleForm(rule: XMLRule) : NodeSeq = {
    <h3>{rule.name}</h3> ++
    <form class="lift:form.ajax">
      <lift:Ajax.addRule>
        requires: <entry:requires value={rule.requires.toString()}/><br />
        provides: <entry:provides value={rule.provides.toString()}/><br />
        runScript: <entry:runScript value={rule.run.toString}/><br />
        <input type="submit" value="Submit" />
      </lift:Ajax.addRule>
    </form>
  }


/*  private def ruleForm(rule: XMLRule) : NodeSeq = {
    <lift:Ajax.addRule form="POST">
      requires: <entry:requires value={rule.requiresList.toString()}/><br />
      provides: <entry:provides value={rule.provides.toString()}/><br />
      runScript: <entry:runscript value={rule.runScript.toString}/><br />
      <entry:submit />
    </lift:Ajax.addRule>
  }
*/

}
