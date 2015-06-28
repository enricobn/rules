package bootstrap.liftweb

import net.liftweb.common.Loggable
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.{JavaScriptResponse, Html5Properties, LiftRules, Req, NoticeType}
import net.liftweb.sitemap.{Menu, SiteMap}
import net.liftweb.common.Full
import net.liftweb.util.Helpers._
import org.rules.lift.RulesInjector
import org.rules.lift.model.{RulesDAO, RulesXMLFileDAO}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable  {

  def boot {
    RulesInjector.registerInjection(() => RulesXMLFileDAO) (Manifest.classType(classOf[RulesDAO]))

    // where to search snippet
    LiftRules.addToPackages("org.rules.lift")

    // Build SiteMap
    def sitemap(): SiteMap = SiteMap(
      Menu.i("Home") / "index"
    )

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    /*
    class OnError(ex: Exception) extends JsCmd {
      override def toJsCmd: String = s"showError('${ex.getMessage}', '${ex.getStackTrace.toString}')"
    }
*/
    LiftRules.exceptionHandler.prepend {
      case (mode, req, ex : Exception) =>
        logger.error("Error from " + req, ex)
        JavaScriptResponse(JsCmds.Alert("Error: " + ex.getMessage))
      //new OnError(ex))
      //ModalDialog(<p>Crap it failed.</p>))
    }

    LiftRules.noticesAutoFadeOut.default.set(
      (notices: NoticeType.Value) =>Full(2 seconds, 2 seconds))

  }
}
