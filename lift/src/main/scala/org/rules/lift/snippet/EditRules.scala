package org.rules.lift.snippet

/**
 * Created by enrico on 6/5/15.
 */
import net.liftweb._
import http._
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.js.JsCmds.{CmdPair, SetHtml}
import org.rules.rule.xml.XMLRule
import scala.xml.{Text, NodeSeq}
import net.liftweb.util.BindHelpers._

/**
 * A RequestVar-based snippet
 */
object EditRules {
  private object ruleVar extends RequestVar[Option[XMLRule]](None)
  private object requiresVar extends RequestVar("")
  private object providesVar extends RequestVar("")
  private object runVar extends RequestVar("")

  def embed() = {
      <span class="lift:embed?what=/editrules" />
  }

  def listRules (xhtml: NodeSeq): NodeSeq = {
    <h2>Rules</h2> ++ Ajax.moduleVar.get.get.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ ruleSeq(rule) ++ <br></br>}
  }


  def processSave() = {
    println("Save requires=" + requiresVar + " provides=" + providesVar + " run=" + runVar)
  }

  def submitRule(xhtml: NodeSeq): NodeSeq = {
    ruleVar.is match {
      case Some (rule) =>
        requiresVar.set(rule.requires.toString())
        providesVar.set(rule.provides.toString())
        runVar.set(rule.runScript.toString)

        val cssTransform = ".requires" #> SHtml.text(requiresVar.is, requiresVar(_)) &
          ".provides" #> SHtml.text(providesVar.is, providesVar(_)) &
          ".runScript" #> SHtml.text(runVar.is, runVar(_))
        cssTransform(xhtml) ++ SHtml.hidden(processSave)
      case _ => Text("Cannot find rule in request")
    }
  }

  def ruleOnClick(node: NodeSeq) : NodeSeq = {
    val cssTransform = ".rule [onclick]" #>
      SHtml.ajaxInvoke(() => {
        val name = node.head.\@("rule-name")
        ruleVar.set(Ajax.moduleVar.get.get.rules.find(_.name == name))
        println("***** click on " + name)
        SetHtml("detail", <span class="lift:embed?what=/ruledetail" />)
      })
    cssTransform(node)
  }

  private def ruleSeq(rule: XMLRule) : NodeSeq = {
    <div data-lift="EditRules.ruleOnClick" rule-name={rule.name}>
      <h3 class="rule">{rule.name}</h3>
      requires: {rule.requiresList.toString()}
      <br></br>
      provides: {rule.xmlProvides.toString()}
      <br></br>
      runScript: {rule.runScript.toString}
    </div>
  }
}