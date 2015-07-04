package org.rules.lift.snippet

/**
 * Created by enrico on 6/5/15.
 */

import net.liftweb._
import http._
import net.liftweb.common.{Failure, Box, Full, Empty}
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsExp._
import net.liftweb.http.js.JsCmds._
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonAST.JValue
import org.rules.lift.{LiftUtils, RulesDAOProvider}
import org.rules.rule.xml.XMLRule
import scala.xml.NodeSeq
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Helpers._

/**
 * A RequestVar-based snippet
 */
object EditRules extends RulesDAOProvider {
  /*
  private object ruleVar extends RequestVar[Option[XMLRule]](None)
  private object requiresVar extends RequestVar("")
  private object providesVar extends RequestVar("")
  private object runVar extends RequestVar("")
*/
  def embed() = {
    val is = getClass().getResourceAsStream("/org/rules/lift/XMLRuleJSONSchema.json")
    val schema = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    <div class="lift:embed?what=/rules-list"></div> ++
    Script(OnLoad( Run(
      s"""
        if (typeof $$.jsonEditor != 'undefined') {
          $$.jsonEditor.destroy();
        }

        if (typeof $$.jsonFromServer == 'undefined') {
          // it's the callback of getting an item from the server, v is the Json value
          $$.jsonFromServer = function(v) {
            console.log('got json from server');
            $$.changedRules.jsonValues[v.id] = v;
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
            $$.changedRules.jsonValues[$$.jsonActiveId] = $$.jsonEditor.getValue();
          }
        });
        $$.changedRules = new Object();

        // the cache of items, key=id value=json
        $$.changedRules.jsonValues = new Object();
        // deleted rules ids
        $$.changedRules.deleted = new Array();
        // the id of the active item
        $$.jsonActiveId = undefined;
        pack();
      """
    )))
  }

}