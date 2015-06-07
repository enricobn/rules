package org.rules.lift.snippet

import java.io.File

import _root_.net.liftweb.http.SHtml._
import net.liftweb.common.Full
import net.liftweb.http.{SessionVar, SHtml, RequestVar}
import net.liftweb.http.js.JsCmd
import org.rules.rule.xml.{XMLProject, XMLModule, XMLRule}
import org.rules.rule.XMLProject

// The next two imports are used to get some implicit conversions
// in scope.
import _root_.net.liftweb.http.js.JsCmds._
import _root_.net.liftweb.util.Helpers._

import scala.xml.{NodeSeq,Text}

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
    Text("requires: ") ++ Text(rule.requiresList.toString()) ++
    <br></br> ++
    Text("provides: ") ++ Text(rule.xmlProvides.toString()) ++
    <br></br> ++
    Text("runScript: ") ++ Text(rule.runScript.toString)
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
        requires: <entry:requires value={rule.requiresList.toString()}/><br />
        provides: <entry:provides value={rule.provides.toString()}/><br />
        runScript: <entry:runScript value={rule.runScript.toString}/><br />
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

/**
 * Created by enrico on 6/1/15.
 */
object Index {

  def header =
    <h2>Rules</h2>

  def nav = Text("")
/*      <div>
        London<br/>
        Paris<br/>
        Tokyo<br/>
      </div>
*/
  def footer =
    <h3>(C) 2015 rules.org</h3>

  def content = Text("")
/*  Range(0,10).foldLeft(NodeSeq.Empty){ (actual,r) =>
    actual ++ <h1>London</h1>
      <p>London is the capital city of England. It is the most populous city in the United Kingdom, with a metropolitan area of over 13 million inhabitants.</p>
      <p>Standing on the River Thames, London has been a major settlement for two millennia, its history going back to its founding by the Romans, who named it Londinium.</p>
      */

}
