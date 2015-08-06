package org.rules.lift

import com.thoughtworks.selenium.Selenium
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium
import net.liftweb.common.Logger
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.openqa.selenium.firefox.FirefoxDriver
import org.scalatest.{FunSuite, BeforeAndAfterAll}

/**
 * Created by enrico on 8/4/15.
 */
class TestSite extends FunSuite with BeforeAndAfterAll with Logger with RulesDAOProvider {
  private var server : Server       = null
  //private var selenium : WebDriver  = null
  private var selenium : Selenium   = null
  private val GUI_PORT              = 8071
  private val host                  = "http://localhost:" + GUI_PORT.toString

  override def beforeAll() {
    // Setting up the jetty instance which will be running the
    // GUI for the duration of the tests
    server  = new Server(GUI_PORT)
    val context = new WebAppContext()

    context.setDescriptor("lift/src/main/webapp/WEB-INF/web.xml")
    context.setResourceBase("lift/src/main/webapp")
    context.setContextPath("/")
    context.setParentLoaderPriority(true)
    server.setHandler(context)
    server.start()

    // Setting up the Selenium Client for the duration of the tests
    val driver = new FirefoxDriver()
    //val driver = new HtmlUnitDriver()
    selenium = new WebDriverBackedSelenium(driver, host)
    selenium.setSpeed("500")
    //selenium.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
  }

  override def afterAll() {
    // Close everyhing when done
    selenium.close()
    server.stop()
  }

  test("Create new project" ) {

    selenium.open("/")

    // check that we are on the right page
    assertResult("Rules") {
      selenium.getTitle
    }

    selenium.click("id=add-project")
    selenium.`type`("//input[@type='text']", "First project")
    selenium.click("css=button.btn.btn-primary")

    // waiting for the button request to be executed on server
    selenium.waitForPageToLoad("1000")

    assert(rulesDAO.getProjects.size == 1)
    assert(rulesDAO.getProjects.head.name == "First project")
  }

}
