package org.rules.rule.xml

import java.io.File

import org.rules.UI
import org.rules.rule.{Logged, Rule, RuleFactory, RuleSolver}
import org.rules.utils.Utils

/**
 * Created by enrico on 6/7/15.
 */
object XMLProject {

   def apply(folder: File) : Logged[XMLProject] = {
     val files = Utils.recursiveListFiles(folder, (f) => f.getName.endsWith(".rules.xml"))
     files.value match {
       case Some(good) => apply(folder.getName, good)
       case _ => files.messages
     }
   }

   def apply(name: String, files: Seq[File]) : XMLProject = {
     val modules = files.map (XMLModule(_))
     new XMLProject(name, modules.toSet)
   }

 }

case class XMLProject(name: String, modules: Set[XMLModule]) {
  val factories = modules.flatMap(_.factories)
  val rules = modules.flatMap(_.rules)

  def solver(ui: UI) = new RuleSolver[String](factories.asInstanceOf[Set[RuleFactory[String]]],
    rules.asInstanceOf[Set[Rule[String]]], ui)
}
