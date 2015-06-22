package org.rules.lift.snippet

import net.liftweb.http.SHtml._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import org.rules.rule.xml.XMLModuleFile

import scala.xml.Text

/**
 * Created by enrico on 6/22/15.
 */
object ProjectMenu {
  def render = {
    def updateModule(id: String) =
    {
      Index.moduleVar.set(Index.projectVar.get.get.modules.find(_.id == id))
      Run("$('#list-rules').show();$('#list-factories').show()") &
      // TODO cleanup of resources
      Run(SetHtml("content", Text("")))
    }

    "#list-modules *" #> Index.projectVar.get.get.modules.map(module =>
      "div [onClick]" #> ajaxInvoke(() => updateModule(module.id)) &
      "div *" #> module.xmlModule.name //&
      //"div [id]" #> module.id
    ) &
    "#list-rules [style+]" #> "display: none;" &
    "#list-rules [onClick]" #> ajaxInvoke(() => Index.updateRules()) &
    "#list-factories [style+]" #> "display: none;"
  }
}
