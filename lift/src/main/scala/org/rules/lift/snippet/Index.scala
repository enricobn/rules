package org.rules.lift.snippet

import java.io.File

import _root_.net.liftweb.http.SHtml._
import net.liftweb.http.RequestVar
import net.liftweb.http.js.JsCmd
import org.rules.rule.{XMLRule, XMLModule, XMLProject}

// The next two imports are used to get some implicit conversions
// in scope.
import _root_.net.liftweb.http.js.JsCmds._
import _root_.net.liftweb.util.Helpers._

import scala.xml.{NodeSeq,Text}

class Ajax {
  private object project extends RequestVar[Option[XMLProject]](None)

  private def updateNav() : JsCmd = {
    val ifProject = XMLProject(new File("core/src/test/resources/org/rules/rule/example3"))

    if (ifProject.value.isEmpty) {
      return Run("alert('Failed to load project');")
    }

    val p = ifProject.value.get

    project.set(Some(p))

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
    val p = project.get
    p.get.modules.foldLeft(NodeSeq.Empty) {(actual,module) => actual ++
      Text(module.name) ++
      <br></br> ++
      ajaxButton("Rules", () => updateRules(module)) ++
      ajaxButton("Factories", () => updateFactories(module))
    }
  }

  private def updateRules(module: XMLModule) : JsCmd = {
    SetHtml("content",
      <h2>Rules</h2> ++ module.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ ruleSeq(rule) ++ <br></br>}
    )
  }

  private def ruleSeq(rule: XMLRule) : NodeSeq = {
    <h3>{rule.name}</h3> ++
    <br></br> ++
    Text("requires: ") ++ Text(rule.requiresList.toString()) ++
    <br></br> ++
    Text("provides: ") ++ Text(rule.provides.toString()) ++
    <br></br> ++
    Text("runScript: ") ++ Text(rule.runScript.toString)
  }

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

  def nav =
      <div>
        London<br/>
        Paris<br/>
        Tokyo<br/>
      </div>

  def footer =
    <h3>(C) 2015 rules.org</h3>

  def content =
  Range(0,10).foldLeft(NodeSeq.Empty){ (actual,r) =>
    actual ++ <h1>London</h1>
      <p>London is the capital city of England. It is the most populous city in the United Kingdom, with a metropolitan area of over 13 million inhabitants.</p>
      <p>Standing on the River Thames, London has been a major settlement for two millennia, its history going back to its founding by the Romans, who named it Londinium.</p>
  }
}
