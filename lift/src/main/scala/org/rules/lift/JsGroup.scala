package org.rules.lift

import net.liftweb.http.js.JE.{JsFunc, Call, JsRaw}
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.http.js.{JsMember, JsExp, JsCmds, JsCmd}
import net.liftweb.util.Helpers._

import scala.xml.{Node, NodeSeq}
import net.liftweb.http.js.JE._ // for implicit conversions

/**
 * Created by enrico on 6/23/15.
 */
/*
trait Group {
  val active : Option[String]

  def find : (String, Node) => Boolean

  def deSelect : Node => JsCmd

  def select : Node => JsCmd

  def activate(xml: NodeSeq, id: String) : JsCmd = {
    var cmd = JsCmds.Noop

    active match {
      case Some(activeId) => val found = xml.find{ node => find(activeId, node) }
        cmd = cmd & deSelect(found.get)
      case _ =>
    }

    val found = xml.find{ node => find(id, node) }
    cmd = cmd & select(found.get)
    return cmd
  }
}

class GroupHideById(idPrefix: String, override val active: Option[String]) extends Group {
  override def select: (Node) => JsCmd =
    (node : Node) => Run(s"$$('#$idPrefix-${node \ "#id"}').show();")

  override def find: (String, Node) => Boolean = (id: String, node: Node) => (node \ "#id") == id

  override def deSelect: (Node) => JsCmd =
    (node : Node) => Run(s"$$('#$idPrefix-${node \ "#id"}').hide();")
}
*/

trait JsGroup {
  def select(id: String) : JsCmd
  def deSelect(id: String) : JsCmd
}

trait JsItemFinder {
  def find(id: String) : JsExp
  def find(id: JsExp) : JsExp
  def getDOMId(id: String) : String
  def getId(domId: String) : String
}

trait JsGroupApplier {
  def applySelect(e: JsExp) : JsCmd
  def applyDeSelect(e: JsExp) : JsCmd
}

case class JsSimpleGroup(finder : JsItemFinder, applier : JsGroupApplier) extends JsGroup {
  def select(id: String) : JsCmd = applier.applySelect(finder.find(id))
  def deSelect(id: String) : JsCmd = applier.applyDeSelect(finder.find(id))
}

case class JQueryById(idPrefix: String) extends JsItemFinder {
  def find(id: String) : JsExp = Call("$", "#" + getDOMId(id))
  def getDOMId(id: String) = s"${idPrefix}_$id"
  def find(id: JsExp) : JsExp = Call("$", JsRaw(s"'#${idPrefix}_'") + id)
  def getId(domId: String) = domId.substring(idPrefix.length + 1)
}

object JQueryHide extends JsGroupApplier {
  def applySelect(e: JsExp) : JsCmd = (e ~> JsFunc("show")).cmd
  def applyDeSelect(e: JsExp) : JsCmd = (e ~> JsFunc("hide")).cmd
}

case class CssClassApplier(cssClass: String) extends JsGroupApplier {
  def applySelect(e: JsExp) : JsCmd = (e ~> JsFunc("addClass", cssClass)).cmd
  def applyDeSelect(e: JsExp) : JsCmd = (e ~> JsFunc("removeClass", cssClass)).cmd
}
