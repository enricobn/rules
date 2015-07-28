package org.rules.lift.utils

import net.liftweb.http.SHtml._
import net.liftweb.http.{S, Templates}
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.json.{Serialization, DefaultFormats}
import net.liftweb.json.JsonAST.JValue
import net.liftweb.util.Helpers._
import org.rules.lift.{JsGroup, JsItemFinder}

import scala.xml._

/**
 * Created by enrico on 7/13/15.
 */
trait LiftListView[T] {

  protected case class State(attributes: Map[String,String], viewId: String, itemFinder: JsItemFinder,
                                   itemsGroup: JsGroup)
  protected case class RenderArgs(state: State, items: Seq[T])

  protected val schemaResource : String

  //protected val templateName : String

  protected def getId(item: T) : String

  protected def renderItem(item: T) : NodeSeq

  def toJson(item: T): JValue

  def fromJson(jsonItem: JValue): T

  protected def renderItems(args: RenderArgs) : NodeSeq => NodeSeq = {
    ".list-elements *" #> args.items.map { item =>
      ".select-item [onClick]" #> ajaxCall(JsRaw(s"""$$.liftViews["${args.state.viewId}"].activeId"""),
        (oldId) => updateItem(oldId, args.state.viewId, args.state.itemsGroup, item)) &
        ".select-item [id]" #> args.state.itemFinder.getDOMId(getId(item)) &
        ".select-item *" #> renderItem(item)
    }
  }

  protected def updateItem(oldId: String, viewId: String, itemsGroup: JsGroup, item: T): JsCmd = {
    val id = getId(item)
    Run(
      s"""
        var view = $$.liftViews['$viewId'];
        view.jsonEditor.disable();
        view.jsonEditor.off('change', view.changeListener);
        view.editingActive = false;
        if (typeof view.cache['$id'] != 'undefined') {
          view.updateEditor(view.cache['$id']);
        } else {
          view.updateEditor(${write(item)});
        }
        if ('$oldId' != 'undefined') {
          ${itemsGroup.deSelect(oldId).toJsCmd}
        }
        ${itemsGroup.select(id).toJsCmd}
      """.stripMargin
    )
  }
  
  protected val fragment =
      <div class="list-main-container" style="height: 100%;">
        <div class="bg2" style="min-height: 50px;">
          <div class="btn btn-primary glyphicon glyphicon-floppy-save save-items"
               style="margin-left: 10px; margin-top: 5px; float: left;"
               data-toggle="tooltip" title="Save"></div>
          <div class="btn btn-primary btn-xs glyphicon glyphicon-plus add-item"
               data-toggle="tooltip" title="Add"
               style="margin-left: 10px; margin-top: 5px; float: left;"></div>
          <div class="btn btn-primary btn-xs glyphicon glyphicon-remove del-item"
               data-toggle="tooltip" title="Delete"
               style="margin-left: 10px; margin-top: 5px; float: left;"></div>
        </div>

        <div class="clear: left; border-bg1 bg3" style="height: 30%; overflow: auto">
          <div class="list-container">
            <div class="list-elements">
              <div class="select-item">item</div>
            </div>
          </div>
        </div>

        <div class="border-bg1 bg3" style="height: 70%; overflow: auto; margin-bottom: 10px; clear: left;">
          <div class="detail-editor" style="margin-left: 10px; margin-right: 10px; margin-top: 10px;">detail editor</div>
        </div>
      </div>

  protected val template = {
    val className : String = {
      val name = getClass.getSimpleName

      if (name.endsWith("$")) {
        name.dropRight(1)
      } else {
        name
      }
    }

    Elem("lift", className, Attribute(None, "style" , Text("height: 100%;"), scala.xml.Null),
      scala.xml.TopScope, false, fragment)
  }

  def embed(attributes: Map[String,String]) = {
/*
    def idEquals(id: String)(node: Node) = {
      node.attributes.exists{ attribute => attribute.key == "id" && attribute.value.text == id }
    }

    val template = Templates("lift-edit-list" :: Nil).openOrThrowException("Cannot find lift-edit-list.html")
    val rulesListRealContent = template \\ "_" filter idEquals("rules_list_real_content") head

    rulesListRealContent match {
      case e : Elem => attributes.foldLeft(e){ (actual, tuple) => actual % Attribute(None, tuple._1, Text(tuple._2), scala.xml.Null) }
      case _ => throw new RuntimeException("Error loading lift-edit-list.html")
    }
*/

    attributes.foldLeft(template){ (actual, tuple) => actual % Attribute(None, tuple._1, Text(tuple._2), scala.xml.Null) }
/*
    def result = <lift:embed what={template}></lift:embed>
    attributes.foldLeft(result){ (actual, tuple) => actual % Attribute(None, tuple._1, Text(tuple._2), scala.xml.Null) }
*/
  }

  protected lazy val schema = {
    val is = getClass().getResourceAsStream(schemaResource)
    scala.io.Source.fromInputStream(is).getLines().mkString("\n")
  }

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
