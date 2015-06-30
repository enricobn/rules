package org.rules.lift

import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.http.S
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Run
import org.rules.lift.snippet.EditRules._
import org.rules.lift.snippet.RulesState

import scala.xml.NodeSeq

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

  /**
   * if the box is Full, returns the evaluation of func, passing in the boxed value, otherwise notifies an S.error
   * with the given message and returns the default
   * @param box
   * @param func
   * @param message
   * @param default
   * @tparam T type of the Boxed value
   * @tparam R type of the return value
   * @return
   */
  def getOrElseError[T,R](box: Box[T], func: (T) => R, message: String, default: R) =
    box match {
      case Full(value) => func(value)
      case Failure(msg, _, _) => S.error(message + ": " + msg); default
      case _ => S.error(message); default
    }
}
