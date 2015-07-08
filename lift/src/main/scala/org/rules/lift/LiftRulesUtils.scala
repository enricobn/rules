package org.rules.lift

import net.liftweb.http.js.JsCmds.Run
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.{JsCmds, JsCmd}

/**
 * Created by enrico on 7/7/15.
 */
object LiftRulesUtils {

  def beforeContentChange(yesCmd : JsCmd, noCmd : JsCmd = JsCmds.Noop) : JsCmd =
    Run(JsRaw(
      s"""
        if (fireBeforeContentChange()) {
          ${yesCmd.toJsCmd}
        } else {
          ${noCmd.toJsCmd}
        }
      """
    ).toJsCmd)
  
}
