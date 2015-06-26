package org.rules.lift

import net.liftweb.http.SHtml._
import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Run

/**
 * Created by enrico on 6/26/15.
 */
object LiftUtils {

  def bootboxPrompt(caption: String, func: (String) => JsCmd) =
    Run(s"""bootbox.prompt("$caption", function(result) {
          if(result != null) {
            ${ajaxCall(JsVar("result"), func)};
          }
        });
        """)

  def bootboxConfirm(caption: String, func: () => JsCmd) =
    Run(s"""bootbox.confirm("${caption}", function(result) {
          if(result) {
            ${ajaxInvoke(func)};
          }
        });
        """)
}
