package org.rules.lift.snippet

import net.liftweb.http.SHtml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._

import scala.xml.Text

/**
 * Created by enrico on 6/22/15.
 */
object ProjectMenu {
  def render = {
    def updateModule(id: String) =
    {
      Index.moduleVar.set(Index.projectVar.get.get.modules.find(_.id == id))

      val js = Index.projectVar.get.get.modules.foldLeft(""){(actual, module) =>
        actual + {
          if (module.id == id) {
            s"$$('#modules-buttons-${module.id}').show();\n"
          } else {
            s"$$('#modules-buttons-${module.id}').hide();\n"
          }
        }
      }

      // TODO cleanup of resources
      Run(SetHtml("content", Text(""))) &
      Run("pack();") &
      Run(js)
    }

    val modules = Index.projectVar.get.get.modules

    "#list-modules *" #> modules.map(module =>
      ".select-module [onClick]" #> ajaxInvoke(() => updateModule(module.id)) &
      ".select-module *" #> module.xmlModule.name &
      ".list-rules [onClick]" #> ajaxInvoke(() => Index.updateRules()) &
      ".modules-buttons [id]" #> ("modules-buttons-" + module.id) &
      ".modules-buttons [style+]" #> "display: none;"
    ) &
    "#project-name *" #> Index.projectVar.get.get.name
  }
}
