package org.rules.lift.snippet

import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import org.rules.lift.{JQueryHide, JQueryById, JQueryGroup, JsGroup}

import scala.xml.Text

/**
 * Created by enrico on 6/22/15.
 * project-menu.html
 */
object ProjectMenu {
  val modulesFinder = JQueryById("modules-buttons")
  val moduleGroup : JQueryGroup = new JQueryGroup(modulesFinder, JQueryHide)

  def render = {

    def updateModule(id: String) =
    {
      val deselect = Index.moduleVar.get match {
        case Some(module) => moduleGroup.deSelect(module.id)
        case _ => Noop
      }
      Index.moduleVar.set(Index.projectVar.get.get.modules.find(_.id == id))

      // TODO cleanup of resources
      Run(SetHtml("content", Text(""))) &
      deselect &
      moduleGroup.select(id) &
      Run("pack();")
      //Run(js)
    }

    val modules = Index.projectVar.get.get.modules

    "#list-modules *" #> modules.map(module =>
      ".select-module [onClick]" #> ajaxInvoke(() => updateModule(module.id)) &
      ".select-module *" #> module.xmlModule.name &
      ".list-rules [onClick]" #> ajaxInvoke(() => Index.updateRules()) &
      ".modules-buttons [id]" #> modulesFinder.getJQueryId(module.id) &
      ".modules-buttons [style+]" #> "display: none;"
    ) &
    "#project-name *" #> Index.projectVar.get.get.name
  }
}
