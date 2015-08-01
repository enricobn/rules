package org.rules.lift.snippet

import net.liftweb.common._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.utils.{LiftListEditor}
import org.rules.rule.xml.XMLRule

import scala.xml.NodeSeq

/**
 * Created by enrico on 6/25/15.
 */
object RulesListEditor extends Loggable with RulesDAOProvider with LiftListEditor[XMLRule] {
  protected val schemaResource = "/org/rules/lift/XMLRuleJSONSchema.json"

  protected def getId(item: XMLRule) = item.id

  def toJson(item: XMLRule): JValue =
    ("id" -> item.id) ~
    ("name" -> item.name) ~
    ("tags" -> item.tags) ~
    ("requires" -> item.requires.map { r => ("token" -> r.token) ~ ("tags" -> r.tags.toString) }) ~
    ("provides" -> item.provides.map { p => ("token" -> p.token) ~ ("value" -> p.value) }) ~
    ("run" -> item.run)

  def fromJson(jsonItem: JValue) : XMLRule = {
    implicit val formats = DefaultFormats
    jsonItem.extract[XMLRule]
  }

  protected def renderItem(item: XMLRule) : NodeSeq =
    <div class="btn btn-primary rules-nav" style="float: left;">{item.name}</div>

  protected def addItem(attributes: Map[String, String]) : XMLRule =
    rulesDAO.createRule(attributes("projectName"), attributes("moduleName"), "New rule")

  protected def save(attributes: Map[String, String], changedItems: List[XMLRule], deletedItems: List[String]) : Box[Unit] =
    rulesDAO.updateRulesAndSave(attributes("projectName"), attributes("moduleName"), changedItems, deletedItems)

  protected def getItemFinder(attributes: Map[String, String]) : JsItemFinder =
    JQueryById(attributes("projectName") + "_" + attributes("moduleName"))

  protected def getItemsGroup(attributes: Map[String, String], itemFinder: JsItemFinder) =
    new JsSimpleGroup(itemFinder, CssClassApplier("active"))

  protected def getItems(attributes: Map[String,String]) =
    rulesDAO.getRules(attributes("projectName"), attributes("moduleName")).openOrThrowException("Error getting rules.")

}