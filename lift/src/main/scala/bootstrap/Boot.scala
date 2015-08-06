package bootstrap.liftweb

import java.io.File

import net.liftweb.common.{Logger, Empty, Loggable, Full}
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http._
import net.liftweb.sitemap.{Menu, SiteMap}
import net.liftweb.util.Helpers._
import net.liftweb.util.Props
import org.rules.lift.RulesInjector
import org.rules.lift.model.{RulesDAO, RulesXMLFileDAO}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable  {

  def boot {

    Props.mode match {
      case Props.RunModes.Test =>
        val root = File.createTempFile("rulestest", "")
        if (!root.delete()) {
          throw new RuntimeException("Cannot delete " + root)
        }
        if (!root.mkdir()) {
          throw new RuntimeException("Cannot create dir " + root)
        }
        logger.info("Test root: " + root)
        RulesInjector.registerInjection(() => RulesXMLFileDAO(root)) (Manifest.classType(classOf[RulesDAO]))
      case _ => RulesInjector.registerInjection(() => RulesXMLFileDAO()) (Manifest.classType(classOf[RulesDAO]))
    }

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
    LiftRules.ajaxStart  =  Full(() => LiftRules.jsArtifacts.show("ajax_spinner").cmd)

    LiftRules.ajaxEnd  =  Full(() => LiftRules.jsArtifacts.hide("ajax_spinner").cmd)

    LiftRules.exceptionHandler.prepend {
      case (mode, req, ex: Exception) => {
        logger.error("Error from " + req, ex)
        JavaScriptResponse(JsCmds.Alert("Error: " + ex.getMessage))
        //new OnError(ex))
        //ModalDialog(<p>Crap it failed.</p>))
      }
    }

    /*
    LiftRules.exceptionHandler.prepend {
      case (runMode, req, exception) => {
        /*
        logger.error("Failed at: "+req.uri, exception)
        RedirectResponse("/500.html")
        val content = S.render(<lift:embed what="500" />, req.request)
       XmlResponse(content.head, 500, "text/html", req.cookies)
       */
        ExceptionResponse(runMode, req, exception)
      }
    }*/

 /*   LiftRules.exceptionHandler.prepend {
      case (runMode, request, exception) =>
        logger.error("Failed at: "+request.uri)
        //InternalServerErrorResponse()
        S.notice(exception.getMessage)
        InMemoryResponse("Hello".getBytes("UTF-8"), request.headers, request.cookies, 500)
    }
*/
    LiftRules.noticesAutoFadeOut.default.set(
      (notices: NoticeType.Value) =>Full(2 seconds, 2 seconds))

    //Logger.setup = Empty

  }
}
