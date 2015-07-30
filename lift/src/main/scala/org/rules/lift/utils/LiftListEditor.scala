package org.rules.lift.utils

import java.util.UUID

import net.liftweb.common.{Failure, Full, Empty, Box}
import net.liftweb.http.SHtml._
import net.liftweb.http._
import net.liftweb.http.js.JE.{Str, Call, JsVar, JsRaw}
import net.liftweb.http.js.{JsExp, JsCmd}
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.{Serialization, DefaultFormats}
import net.liftweb.json.JsonAST.{JString, JArray, JObject, JValue}
import net.liftweb.util.Helpers._
import org.rules.lift._
import org.rules.lift.utils.LiftUtils._

import scala.xml._

/**
 * Created by enrico on 7/13/15.
 */
trait LiftListEditor[T] {

  protected case class State(attributes: Map[String,String], viewId: String, itemFinder: JsItemFinder,
                                   itemsGroup: JsGroup)
  protected case class RenderArgs(state: State, items: Seq[T])

  protected val schemaResource : String

  protected def getId(item: T) : String

  protected def renderItem(item: T) : NodeSeq

  def toJson(item: T): JValue

  def fromJson(jsonItem: JValue): T

  protected def save(attributes: Map[String, String], changedItems: List[T], deletedItems: List[String]) : Box[Unit]

  protected def getItemFinder(attributes: Map[String, String]) : JsItemFinder

  protected def getItemsGroup(attributes: Map[String, String], itemFinder: JsItemFinder) : JsGroup

  protected def getItems(attributes: Map[String,String]) : Seq[T]

  protected def addItem(attributes: Map[String, String]) : T

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
    println(getClass.getSimpleName + ".embed " + attributes)
    val viewId = UUID.randomUUID().toString
    val fragment = attributes.foldLeft(template){ (actual, tuple) => actual % Attribute(None, tuple._1, Text(tuple._2), scala.xml.Null) }
    EmbeddedLiftListEditor(viewId, fragment % Attribute(None, "viewId", Text(viewId), scala.xml.Null))
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
        """.stripMargin)
    LiftUtils.bootboxConfirm(message, ok)
  }

  protected object renderItemsVar extends RequestVar[Option[MemoizeTransformWithArg[RenderArgs]]](None)

  protected def save(state: State) : String =
    SHtml.jsonCall(JsRaw(s"editChanges('${state.viewId}')"), new JsContext(Empty, Empty), (changes: JValue) => {
      val changedItems = changes \ "changed" match {
        case JObject(x :: xs) =>
          val l = x :: xs
          l.foldLeft(List.empty[T]){ (actual, field) => actual.:+(fromJson(field.value))}
        case _ => List.empty[T]
      }

      val deletedItems = changes \ "deleted" match {
        case JArray(l: List[JString]) =>
          l.foldLeft(List.empty[String]){ (actual, field) => actual.:+(field.s)}
        case _ => List.empty[String]
      }

      save(state.attributes, changedItems, deletedItems) match {
        case Full(result) => S.notice("Save succeeded")
        case Failure(msg, _, _) => S.error("Save error: " + msg)
        case _ => S.error("Save error")
      }
      Run(s"editAfterSave('${state.viewId}');")
    })._2.toJsCmd

  private def addItem(state: State) : JsCmd = {
    val item = addItem(state.attributes)
    val renderedItem = renderItemsVar.is.get.applyAgain(RenderArgs(state, Seq(item))) \ "_"
    val id = getId(item)
    Run(
      s"""
      var view = $$.liftViews['${state.viewId}'];
      view.cache['$id'] = ${write(item)};
      view.changed['$id'] = '$id';
      $$('#${state.viewId} .list-container').append(${encJs(renderedItem.toString)});
      ${state.itemFinder.find(id).toJsCmd}.trigger('click');
    """.stripMargin
    )
  }

  private def onEditorChange(state: State, json: JValue) = {
    val item = fromJson(json)
    val id = getId(item)
    val renderedItem = renderItemsVar.is.get.applyAgain(RenderArgs(state, Seq(item))) \ "_"
    Run(s"${state.itemFinder.find(id).toJsCmd}.replaceWith(${encJs(renderedItem.toString)});") &
      state.itemsGroup.select(id)
  }

  def hasUnsavedChanges(viewId: JsExp) : JsExp =
    JsRaw(
      s"""
        (function () {
          if (typeof $$.liftViews == 'undefined') {
             return false;
          } else {
            return $$.liftViews[${viewId.toJsCmd}].hasUnsavedChanges();
          }
        } ())
      """.stripMargin
    )

  def render() = {
    val attributes = S.attrs.map( attr =>
      attr match {
        case (Left(key), value) => (key -> value)
        case (Right((namespace, key)), value) => (namespace + ":" + key -> value)
      }
    ).toMap

    val viewId = attributes("viewId")

    val itemFinder = getItemFinder(attributes)
    val itemsGroup = getItemsGroup(attributes, itemFinder)

    val state = State(attributes, viewId, itemFinder, itemsGroup)

    val items : Seq[T] = getItems(attributes)

    renderItemsVar.set(Some(memoizeWithArg(renderItems)))

    S.appendJs(
      Run(
        s"""$$('[data-toggle="tooltip"]').tooltip();
            editInit('$viewId', $$("#$viewId .detail-editor"), $schema, function(oldJson, newJson) {
              if (newJson.name != oldJson.name) {
                ${jsonCall(JsVar("newJson"), (json : JValue) => onEditorChange(state, json))._2.toJsCmd}
              }
            });
         """.stripMargin))

    ".list-main-container [id]" #> viewId &
    ".list-container *" #> renderItemsVar.is.get.apply(RenderArgs(state, items)) &
    ".add-item [onClick]" #> ajaxCall("undefined", (_) => addItem(state)) &
    ".del-item [onClick]" #> delItem("Are you sure to delete current item?", state) &
    ".save-items [onclick]" #> save(state)
  }
  
}
