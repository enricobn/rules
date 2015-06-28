package org.rules.lift.snippet

import java.io.File

import net.liftweb.http.js.JE.{JsRaw, JsFunc, Call, ElemById}
import net.liftweb.http.{SessionVar, SHtml}
import net.liftweb.http.SHtml._
import net.liftweb.util.CssSel
import net.liftweb.util.Helpers._
import net.liftweb.http.js.{JsCmds, JsCmd}
import net.liftweb.http.js.JsCmds._
import org.rules.lift.{RulesDAOProvider, CmdList}
import org.rules.lift.model.RulesDAO
import org.rules.rule.xml.{XMLProjectFile, XMLModuleFile, XMLModule, XMLProject}

import scala.xml.{NodeSeq, Text}



/**
 * Created by enrico on 6/1/15.
 */
object Index extends RulesDAOProvider {
  private object projectVar extends SessionVar[Option[String]](None)
  // TOD better if it's only an id
  private object moduleVar extends SessionVar[Option[String]](None)

  def currentProjectName = projectVar.get

  def currentModuleName = moduleVar.get

  def setCurrentProjectName(name: String) = projectVar.set(Some(name))

  def setCurrentModuleName(name: String) = moduleVar.set(Some(name))

  def updateRules() : JsCmd = {
    //val module = moduleVar.get.get
    //jsonEditor(module)
    CmdPair(
      SetHtml("content",
        EditRules.embed
        //  <h2>Rules</h2> ++ module.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ ruleForm(rule) ++ <br></br>}
      ),
      Run("$.ruleEditor = undefined;")
    )
  }
/*
  private def updateFactories(module: XMLModule) : JsCmd = {
    SetHtml("content",
      <h2>Factories</h2> ++ module.factories.foldLeft(NodeSeq.Empty) {(actual,factory) => actual ++ Text(factory.name) ++ <br></br>}
    )
  }

  private def modules() : NodeSeq = {
    val p = projectVar.get
    p.get.modules.foldLeft(NodeSeq.Empty) {(actual,module) => actual ++
      <h5 class="rules-nav">{module.xmlModule.name}</h5> ++
      rulesButton(module) ++
      <br></br> ++
      factoriesButton(module)
    }
  }

  def updateRulesButton(module: XMLModuleFile) : JsCmd =
    Replace(rulesButtonId(module), rulesButton(module))

  def updateFactoriesButton(module: XMLModuleFile) : JsCmd =
    Replace(factoriesButtonId(module), factoriesButton(module))

  private def rulesButtonId(module: XMLModuleFile) = "rules_button_" + module.id

  private def factoriesButtonId(module: XMLModuleFile) = "factories_button_" + module.id

  private def rulesButton(module: XMLModuleFile) =
    ajaxButton("Rules", () => updateRules(module), ("class", "btn btn-primary rules-nav"), ("id", rulesButtonId(module)))

  private def factoriesButton(module: XMLModuleFile) =
    ajaxButton("Factories", () => updateRules(module), ("class", "btn btn-primary rules-nav"), ("id", factoriesButtonId(module)))
*/
  private def updateNav(folder: File) : JsCmd = {
    val ifProject = XMLProjectFile.open(folder)

    if (ifProject.value.isEmpty) {
      return Run("alert('Failed to load project');")
    }

    val p = ifProject.value.get

    setCurrentProjectName(p.name)

    SetHtml("project-menu", <span class="lift:embed?what=/project-menu" />) &
    SetHtml("content", Text("")) &
    Run("pack();")

/*    CmdPair(
      updateProjectMenu(p),
      SetHtml("content", Text(""))
    )
*/
/*    projectVar.set(Some(p))

    CmdPair(
      CmdPair(
        SetHtml("project-menu", <span class="lift:embed?what=/project-menu" />
  /*
          <h2 class="rules-nav">{p.name}</h2> ++
            modules()
*/
          //ajaxButton("Rules", () => updateRules()) ++
          //<br></br> ++
          //ajaxButton("Factories", () => updateFactories())
        ),
        SetHtml("content", Text(""))),
      Run("pack();")
    )
    */
  }

  /**
   * TODO I don't like it
   * it checks if the deleted project is the current one, if true it repaints project-menu and content
   * @param name
   */
  def projectDeleted(name: String) = {
    if (projectVar.get.isDefined && projectVar.get.get.name == name) {
      projectVar.set(None)
      SetHtml("project-menu", Text("")) &
        SetHtml("content", Text("")) &
        Run("pack();")
    } else {
      Noop
    }
  }

  /**
   * TODO I don't like it
   * it checks if the deleted module is the current one, if true it repaints content
   * @param name
   */
  def moduleDeleted(name: String) = {
    if (moduleVar.get.isDefined && currentModuleName == name) {
      moduleVar.set(None)
      SetHtml("content", Text("")) &
        Run("pack();")
    } else {
      Noop
    }
  }

  def updateListProjects() = {
    SetHtml("list-projects", <span class="lift:embed?what=/list-projects" />) &
    Run("pack();")
  }

  def updateProjectMenu(project: XMLProject) = {
      SetHtml("project-menu", <span class="lift:embed?what=/project-menu" />) &
      Run("pack();"
    )
  }

  def render() = {
    def showProjectName() = {
      SetValById("add-project-name", "new project") &
      JsShowId("add-project-name") &
      Run("pack();")
    }

    def addProject(name: String) = {
      rulesDAO.createProject(name)

      JsHideId("add-project-name") &
      // TODO I don't like it since if I change the template (index.html) this must be changed as well!
      Run(s"""$$('#list-projects-container').append("<div class='btn btn-primary rules-nav'>$name</div><br/>");""") &
      Run("pack();")
    }


    /*
    def listProjects(in: NodeSeq) : NodeSeq = {
      new File("data").listFiles().filter(_.isDirectory).foldLeft(NodeSeq.Empty) { (actual, folder) =>
        actual ++ ajaxButton(Text(folder.getName), () => updateNav(folder), ("class", "btn btn-primary rules-nav")) ++ <br />
      }
      //a(() => showNav, in)
    }
    */
    "#project-menu *" #> Text("")
  }
}
