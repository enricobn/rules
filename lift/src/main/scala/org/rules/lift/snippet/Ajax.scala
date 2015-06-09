package org.rules.lift.snippet

import java.io.File
import java.nio.file.Files

import org.rules.rule.xml.{XMLRule, XMLModule, XMLProject}
import net.liftweb.http.{SHtml, SessionVar}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.{SetHtml, CmdPair, Run}
import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml._
import scala.xml.{NodeSeq, Text}
// The next two imports are used to get some implicit conversions
// in scope.
import _root_.net.liftweb.http.js.JsCmds._

/**
 * Created by enrico on 6/7/15.
 */
object Ajax {
  object projectVar extends SessionVar[Option[XMLProject]](None)
  object moduleVar extends SessionVar[Option[XMLModule]](None)

  private def updateNav() : JsCmd = {
    val ifProject = XMLProject(new File("core/src/test/resources/org/rules/rule/example3"))

    if (ifProject.value.isEmpty) {
      return Run("alert('Failed to load project');")
    }

    val p = ifProject.value.get

    projectVar.set(Some(p))

    CmdPair(
      CmdPair(
        SetHtml("project-menu",
          <h2>{p.name}</h2> ++
          modules()
          //ajaxButton("Rules", () => updateRules()) ++
          //<br></br> ++
          //ajaxButton("Factories", () => updateFactories())
        ),
        SetHtml("content", Text(""))),
      Run("pack();")
    )
  }

  private def modules() : NodeSeq = {
    val p = projectVar.get
    p.get.modules.foldLeft(NodeSeq.Empty) {(actual,module) => actual ++
      Text(module.name) ++
      <br></br> ++
      ajaxButton("Rules", () => updateRules(module)) ++
      ajaxButton("Factories", () => updateFactories(module))
    }
  }

  private def updateRules(module: XMLModule) : JsCmd = {
    moduleVar.set(Some(module))
    SetHtml("content",
      EditRules.embed
    //  <h2>Rules</h2> ++ module.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ ruleForm(rule) ++ <br></br>}
    )
  }

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

  private def updateFactories(module: XMLModule) : JsCmd = {
    SetHtml("content",
      <h2>Factories</h2> ++ module.factories.foldLeft(NodeSeq.Empty) {(actual,factory) => actual ++ Text(factory.name) ++ <br></br>}
    )
  }


  def render = {
    // build up an ajax button to show the rules in the navigation div
    def openProject(in: NodeSeq) : NodeSeq = {
      ajaxButton(Text("Load project"), () => updateNav)
      //a(() => showNav, in)
    }

    // I bind the openProject method to element with id 'open-project'
    "#open-project" #> openProject _
  }
}