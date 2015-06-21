package org.rules.lift.snippet

/**
 * Created by enrico on 6/5/15.
 */

import net.liftweb
import net.liftweb._
import http._
import net.liftweb.common.{Full, Empty}
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JE.{JsObj, JsRaw}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsExp._
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonAST.JValue
import org.rules.rule.xml.{XMLProvides, XMLModule, XMLRule}
import scala.xml.{Elem, Text, NodeSeq}
import net.liftweb.util.BindHelpers._
import net.liftweb.json.Xml.{toJson, toXml}
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

/**
 * A RequestVar-based snippet
 */
object EditRules {
  /*
  private object ruleVar extends RequestVar[Option[XMLRule]](None)
  private object requiresVar extends RequestVar("")
  private object providesVar extends RequestVar("")
  private object runVar extends RequestVar("")
*/
  def embed() = {
    val is = getClass().getResourceAsStream("/org/rules/lift/XMLRuleJSONSchema.json")
    val schema = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    <div class="bg1" style="height: 5%;">
      <button class="btn btn-primary btn-sm" style="margin-left: 10px; margin-top: 5px; float: left;">New</button>
      <button data-lift="EditRules.saveButton" class="btn btn-primary btn-sm" style="margin-left: 10px; margin-top: 5px; float: left;">Save</button>
    </div>
    <div id="list" class="border-bg1 bg2" style="height: 30%; overflow: auto;">
      <div data-lift="EditRules.listRules" />
    </div>
    <div id="detail" class="border-bg1 bg2" style="height: 65%; overflow: auto;">
      <div id="detail-editor" style="margin-left: 10px; margin-right: 10px; margin-top: 10px;"></div>
    </div> ++
    Script(OnLoad( Run(
      s"""
        if (typeof $$.jsonEditor != 'undefined') {
          $$.jsonEditor.destroy();
        }

        if (typeof $$.jsonFromServer == 'undefined') {
          // it's the callback of getting an item from the server, v is the Json value
          $$.jsonFromServer = function(v) {
            console.log('got json from server');
            $$.jsonValues[v.id] = v;
            $$.jsonEditor.setValue(v);
            $$.jsonActiveId =  v.id;
            $$("#detail-editor").show();
          };

          // it's the callback of the error getting an item from the server
          $$.jsonFromServerFailure = function() {
            console.log('Error getting json from server');
            // TODO do it better
            alert('Error getting json from server.');
          };
        }

        JSONEditor.defaults.options.theme = 'bootstrap3';
        JSONEditor.defaults.iconlib = 'bootstrap3';
        JSONEditor.defaults.options.disable_edit_json = true;
        JSONEditor.defaults.options.disable_properties = true;
        JSONEditor.defaults.options.disable_collapse = true;

        $$("#detail-editor").empty();

        $$("#detail-editor").hide();

        $$.jsonEditor = new JSONEditor(document.getElementById("detail-editor"), $schema);

        // to hide the title of the editor
        $$( "span:contains('hide-me')" ).parent().hide();

        // on change values in the editor, I update the cache
        $$.jsonEditor.on('change', function() {
          if (typeof $$.jsonActiveId != 'undefined') {
            $$.jsonValues[$$.jsonActiveId] = $$.jsonEditor.getValue();
          }
        });

        // the cache of items, key=id value=json
        $$.jsonValues = new Object();

        // the id of the active item
        $$.jsonActiveId = undefined;
        pack();
      """
    )))
  }

  /*
  private def jsonEditor(rule: XMLRule) : JsCmd = {
    lazy val jsonRule = json.compact(json.render(ruleToJson(rule)))

    Run(
      s"""
      $$.jsonEditor.setValue($jsonRule);
      $$.getJson('id');
      """
    )
  }
*/

  def saveButton (node: NodeSeq): NodeSeq = {
    val cssTransform = "button [onclick]" #>
          SHtml.jsonCall(JsRaw("$.jsonValues"), new JsContext(Empty, Empty), (values: JValue)=>{
//            println(values)
            val rules = values match {
              case JObject(x :: xs) =>
                val l = x :: xs
                l.foldLeft(List.empty[XMLRule]){ (actual, field) => actual.:+(jsonToRule(field.value))}
              //l.foreach{ field => println("##### Saved " + jsonToRule(field))}
              case _ => List.empty[XMLRule]
            }

            val updatedModule = Index.moduleVar.get.get.updateAndSave(rules)

            Index.moduleVar.set(Some(updatedModule))

            val updatedProject = Index.projectVar.get.get.updateModule(updatedModule)
            Index.projectVar.set(Some(updatedProject))

            S.notice("Save succeded")

            // TODO remove when you'll put the current module in session (and update it on save)
            Index.updateRulesButton(updatedModule)
            //)
          })._2.toJsCmd

    cssTransform(node)
    /*
    SHtml.jsonButton(Text("Save"), JsRaw("$.jsonValues"), (values : JValue) => {
      println(values)
      val rules = values match {
        case JObject(x :: xs) =>
          val l = x :: xs
          l.foldLeft(List.empty[XMLRule]){ (actual, field) => actual.:+(jsonToRule(field))}
          //l.foreach{ field => println("##### Saved " + jsonToRule(field))}
        case _ => List.empty[XMLRule]
      }

      val updatedModule = Index.moduleVar.get.get.updateAndSave(rules)

      Index.moduleVar.set(Some(updatedModule))

      val updatedProject = Index.projectVar.get.get.updateModule(updatedModule)
      Index.projectVar.set(Some(updatedProject))

      /*
      CmdPair(
      SetHtml("module_" + updatedModule.xmlModule.name,
        ajaxButton("Rules", () => Index.updateRules(updatedModule), ("class", "btn btn-default rules-nav"), ("id","module_" + updatedModule.xmlModule.name)))
      , JsRaw("{\"result\": \"OK\"}"))
      */
      new JsCmd {
        def toJsCmd = JsObj(("result", "OK")).toJsCmd
      }

      //Noop
    }, new JsonContext(Empty, Empty))
    */
  }

  def listRules (xhtml: NodeSeq): NodeSeq = {
    Index.moduleVar.get.get.xmlModule.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ render(rule)}
  }

  /*
  def processSave() = {
    println("Save requires=" + requiresVar + " provides=" + providesVar + " run=" + runVar)
  }
*/
  /*
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
*/

  private def getRule(id: String) = Index.moduleVar.get.get.xmlModule.rules.find(_.id == id).get

  def ruleOnClick(node: NodeSeq) : NodeSeq = {
    val id = node.head.\@("rule-id")

    val cssTransform = ".rule [onclick]" #>
      Script(JsRaw(
        s"""
          if (typeof $$.jsonValues['$id'] != 'undefined') {
            $$.jsonEditor.setValue($$.jsonValues['$id']);
            $$.jsonActiveId = '$id';
          } else {
        """ +
        SHtml.jsonCall(id, new JsonContext(Full("$.jsonFromServer"), Full("$.jsonFromServerFailure")), (a:Any)=>{
          ruleToJson(getRule(id))
        })._2.toJsCmd
        + "}\npack();"
      ))


//    jsonEditor(getRule(name))
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

    /*
    class FooSerializer extends Serializer[XMLRule] {
      private val Class = classOf[XMLRule]

      def deserialize(implicit format: Formats): PartialFunction[(TypeInfo,
         JValue), XMLRule] = {
        case (TypeInfo(Class, _), json) => json match {
          case JString(s) =>
            println("deserialize " + s)
            s
          case x => json.extract(x)
        }
      }

      def serialize(implicit format: Formats): PartialFunction[Any, liftweb.json.JValue] =
      {
        case f: String => JString(f)
        case x => throw new MappingException("Can't convert " + x + " to JString")
      }
    }
    */
/*
    def rawStringSe(name: String): PartialFunction[(String, Any), Option[(String, Any)]] = {
      case (`name`, x : String) => Some(`name`, x)
    }

    def rawStringDe(name: String): PartialFunction[JField, JField] = {
      case JField(`name`, x) => JField(`name`, x)
    }
*/
    implicit val formats = DefaultFormats //+ FieldSerializer[XMLRule]() + FieldSerializer[XMLRule](rawStringSe("run"), rawStringDe("run"))


    //println(compact(JsonAST.render(("run" -> "hello\nciupa")))) //{\"run\":\"hello\"}"))

    //println(Serialization.write(json))
    //println(Serialization.read[XMLRule](compactRender(json)))

    json.extract[XMLRule]
  }

  def optionToString(s: Option[String]) : String =
    s match {
      case Some(s) => s
      case _ => ""
    }

  def ruleToJson(rule: XMLRule) : JValue = {
    ("id" -> rule.id) ~
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
    ("run" -> rule.run)
  }

  private def render(rule: XMLRule) : NodeSeq = {
/*    println(rule.toXML())

    val json = ruleToJson(rule)

    println(json)

    println(pretty(render(json)))

    println(jsonToRule(json).toXML())
    */

    <div data-lift="EditRules.ruleOnClick" rule-id={rule.id}>
      <span class="btn btn-info rule" style="margin-top: 10px; margin-left: 10px; float: left;">{rule.name}</span>
    </div>
  }
}