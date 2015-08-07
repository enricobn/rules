package org.rules.lift

import java.util.concurrent.TimeUnit

import com.thoughtworks.selenium.Selenium
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium
import net.liftweb.common.Logger
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.openqa.selenium.{WebElement, By, WebDriver}
import org.openqa.selenium.firefox.FirefoxDriver
import org.scalatest.{FunSuite, BeforeAndAfterAll}

import scala.collection.JavaConverters
import scala.runtime.ScalaRunTime

/**
 * Created by enrico on 8/4/15.
 */
class TestSite extends FunSuite with BeforeAndAfterAll with Logger with RulesDAOProvider {
  private var server : Server       = null
  private var driver : WebDriver    = null
  private val GUI_PORT              = 8071
  private val baseUrl               = "http://localhost:" + GUI_PORT.toString

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

    // Setting up the driver for the duration of the tests
    driver = new FirefoxDriver()
    driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS)
  }

  override def afterAll() {
    // Close everyhing when done
    driver.close()
    server.stop()
  }

  test("Create new project" ) {
    driver.get(baseUrl + "/")

    // check that we are on the right page
    assertResult("Rules") {
      driver.getTitle
    }

    val selectProject = new Monitor(driver, By.className("select-project"))

    val testProject = selectProject.run(() => {
      driver.findElement(By.id("add-project")).click()
      bootboxPrompt(driver, "Test project")
    }).newOne

    val addToList = new Monitor(driver, By.className("add-to-list"))
    val addToProject = addToList.run(() => {
      testProject.click()
    }).newOne

    val selectItem = new Monitor(driver, By.className("select-item"))
    val testModule = selectItem.run( () => {
        addToProject.click()
        bootboxPrompt(driver, "Test module")
    }).newOne

    val addSaveItems = new MulMonitor(driver, Map("add" -> By.className("add-item"), "save" -> By.className("save-items")))
    val ruleTab = addSaveItems.run( () => {
      testModule.click()
    })

    // it's equals to modules
    //val rules = new Monitor(driver, By.className("select-item"))
    val testRule = selectItem.run( () => {
      ruleTab.newOne("add").click()
    }).newOne

    testRule.click()
    ruleTab.newOne("save").click()

    // waiting for the requests to be executed on server
    Thread.sleep(1000)

    assert(rulesDAO.getProjects.size == 1)
    assert(rulesDAO.getProjects.head.name == "Test project")
    assert(rulesDAO.getProjects.head.modules.head.name == "Test module")
    assert(rulesDAO.getProjects.head.modules.head.rules.size == 1)
  }

  def bootboxPrompt(driver: WebDriver, value: String) = {
    driver.findElement(By.xpath("//input[@type='text']")).clear()
    driver.findElement(By.xpath("//input[@type='text']")).sendKeys(value)
    driver.findElement(By.cssSelector("button.btn.btn-primary")).click()
  }

  def findElements(driver: WebDriver, by: By) =
    JavaConverters.collectionAsScalaIterableConverter(driver.findElements(by)).asScala.toSet

  case class MonitorResult(newElements: Set[WebElement], delElements: Set[WebElement]) {
    def newOne = newElements.head
  }

  class Monitor(driver: WebDriver, by: By) {

    def run(fun: () => Unit, sleep: Int = 500) = {
      val elements = findElements(driver, by)
      fun()
      Thread.sleep(sleep)
      val actualElements = findElements(driver, by)

      val newElements = actualElements.diff(elements)
      val delElements = elements.diff(actualElements)
      MonitorResult(newElements, delElements)
    }

  }

  case class MulMonitorResult(newElements: Map[String,Set[WebElement]], delElements: Map[String,Set[WebElement]]) {
    def newOne(key: String) = newElements(key).head
  }

  class MulMonitor(driver: WebDriver, by: Map[String,By]) {

    def run(fun: () => Unit, sleep: Int = 500) = {
      val elements = by.map{ e => (e._1, findElements(driver, e._2)) }
      fun()
      Thread.sleep(sleep)
      val actualElements = by.map{ e => (e._1, findElements(driver, e._2)) }

      val newElements = actualElements.map{ e => (e._1, e._2.diff(elements(e._1))) }
      val delElements = elements.map{ e => (e._1, e._2.diff(actualElements(e._1))) }
      MulMonitorResult(newElements, delElements)
    }

  }

}
