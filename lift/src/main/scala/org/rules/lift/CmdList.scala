package org.rules.lift

import net.liftweb.http.js.JsCmds.CmdPair
import net.liftweb.http.js.{JsCmds, JsCmd}

/**
 * Created by enrico on 6/22/15.
 */
case class CmdList(cmds: JsCmd*) extends JsCmd {
  override def toJsCmd: String = {
    if (cmds.isEmpty) {
      JsCmds.Noop.toJsCmd
    } else if (cmds.length == 1) {
      cmds.head.toJsCmd
    } else if (cmds.length == 2) {
      CmdPair(cmds.head, cmds.tail.head)
    } else {
      cmds.tail.tail.foldLeft(CmdPair(cmds.head, cmds.tail.head)){(actual, cmd) => CmdPair(actual, cmd)}
    }
  }
}
