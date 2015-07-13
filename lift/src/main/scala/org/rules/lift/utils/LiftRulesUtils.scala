package org.rules.lift.utils

import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.http.js.{JsCmd, JsCmds}

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
