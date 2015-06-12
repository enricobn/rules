package org.rules.lift.snippet

/**
 * Created by enrico on 6/5/15.
 */
import net.liftweb._
import http._
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.js.JsCmds.{CmdPair, SetHtml}
import net.liftweb.json.JsonAST._
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
  private object ruleVar extends RequestVar[Option[XMLRule]](None)
  private object requiresVar extends RequestVar("")
  private object providesVar extends RequestVar("")
  private object runVar extends RequestVar("")

  def embed() = {
      <span class="lift:embed?what=/editrules" />
  }

  def listRules (xhtml: NodeSeq): NodeSeq = {
    <h2 style="margin-left: 10px;">Rules</h2> ++
    Ajax.moduleVar.get.get.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ ruleSeq(rule)}
  }


  def processSave() = {
    println("Save requires=" + requiresVar + " provides=" + providesVar + " run=" + runVar)
  }

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

  def ruleOnClick(node: NodeSeq) : NodeSeq = {
    val name = node.head.\@("rule-name")

    val cssTransform = ".rule [onclick]" #>
      Ajax.jsonEditor(Ajax.moduleVar.get.get.rules.find(_.name == name).get)
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

  private def ruleSeq(rule: XMLRule) : NodeSeq = {
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