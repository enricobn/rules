package org.rules.lift.snippet

import java.io.File

import net.liftweb.http.{SessionVar, SHtml}
import net.liftweb.http.SHtml._
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.{SetHtml, CmdPair, Run}
import org.rules.rule.xml.{XMLModuleFile, XMLModule, XMLProject}

import scala.xml.{NodeSeq, Text}



/**
 * Created by enrico on 6/1/15.
 */
object Index {
  object projectVar extends SessionVar[Option[XMLProject]](None)
  object moduleVar extends SessionVar[Option[XMLModuleFile]](None)

  def updateRules(module: XMLModuleFile) : JsCmd = {
    moduleVar.set(Some(module))
    //jsonEditor(module)
    CmdPair(
      SetHtml("content",
        EditRules.embed
        //  <h2>Rules</h2> ++ module.rules.foldLeft(NodeSeq.Empty) {(actual,rule) => actual ++ ruleForm(rule) ++ <br></br>}
      ),
      Run("$.ruleEditor = undefined;")
    )
  }

  private def updateFactories(module: XMLModule) : JsCmd = {
    SetHtml("content",
      <h2>Factories</h2> ++ module.factories.foldLeft(NodeSeq.Empty) {(actual,factory) => actual ++ Text(factory.name) ++ <br></br>}
    )
  }

  private def modules() : NodeSeq = {
    val p = projectVar.get
    p.get.modules.foldLeft(NodeSeq.Empty) {(actual,module) => actual ++
      <h5 class="rules-nav">{module.xmlModule.name}</h5> ++
      ajaxButton("Rules", () => updateRules(module), ("class", "btn btn-default rules-nav"), ("id","module_" + module.xmlModule.name)) ++
      <br></br> ++
      ajaxButton("Factories", () => updateFactories(module.xmlModule), ("class", "btn btn-default rules-nav"))
    }
  }

  private def updateNav(folder: File) : JsCmd = {
    val ifProject = XMLProject(folder)

    if (ifProject.value.isEmpty) {
      return Run("alert('Failed to load project');")
    }

    val p = ifProject.value.get

    projectVar.set(Some(p))

    CmdPair(
      CmdPair(
        SetHtml("project-menu",
          <h2 class="rules-nav">{p.name}</h2> ++
            modules()
          //ajaxButton("Rules", () => updateRules()) ++
          //<br></br> ++
          //ajaxButton("Factories", () => updateFactories())
        ),
        SetHtml("content", Text(""))),
      Run("pack();")
    )
  }

  def render = {
    // build up an ajax button to show the rules in the navigation div
    def newProject(in: NodeSeq) : NodeSeq = {
      Text("")
      //<button>New project</button>
      //a(() => showNav, in)
    }

    def openProject(in: NodeSeq) : NodeSeq = {
      new File("data").listFiles().filter(_.isDirectory).foldLeft(NodeSeq.Empty) { (actual, folder) =>
        actual ++ ajaxButton(Text(folder.getName), () => updateNav(folder)) ++ <br />
      }
      //a(() => showNav, in)
    }
    // I bind the openProject method to element with id 'open-project'
    "#open-project" #> openProject _ &
    "#new-project" #> newProject _
  }
}
