package org.rules.lift.utils

import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.json.{Serialization, DefaultFormats}
import net.liftweb.json.JsonAST.JValue
import org.rules.lift.{JsGroup, JsItemFinder}

import scala.xml.{Text, Attribute}

/**
 * Created by enrico on 7/13/15.
 */
trait LiftListView[T] {

  protected case class State(attributes: Map[String,String], viewId: String, itemFinder: JsItemFinder,
                                   itemsGroup: JsGroup)
  protected case class RenderArgs(state: State, items: Seq[T])

  protected val schemaResource : String

  protected val template : String

  def embed(attributes: Map[String,String]) = {
    def result = <lift:embed what={template}></lift:embed>
    attributes.foldLeft(result){ (actual, tuple) => actual % Attribute(None, tuple._1, Text(tuple._2), scala.xml.Null) }
  }

  protected lazy val schema = {
    val is = getClass().getResourceAsStream(schemaResource)
    scala.io.Source.fromInputStream(is).getLines().mkString("\n")
  }

  def toJson(item: T): JValue

  def fromJson(jsonItem: JValue): T

  protected def write(item: T) : String = {
    write(toJson(item))
  }

  protected def write(jsonItem: JValue) : String = {
    implicit val formats = DefaultFormats
    Serialization.write(jsonItem)
  }

  protected def delItem(message: String, state: State) = {
    val ok = Run(
      s"""
           var view = $$.liftViews['${state.viewId}'];
           if (typeof view.activeId != 'undefined') {
              view.editingActive = false;
              ${state.itemFinder.find(JsRaw("view.activeId")).toJsCmd}.hide();
              view.deleted.push(view.activeId);
              $$("#${state.viewId} .detail-editor").hide();
              view.activeId = undefined;
            }
        """)
    LiftUtils.bootboxConfirm(message, ok)
  }




}
