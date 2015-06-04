package org.rules.rule

import java.io.{FileInputStream, InputStream}
import javax.script.{ScriptEngine, ScriptEngineManager}

import org.rules.{JavaUtils, UI}

import scala.collection.generic.IsTraversableLike
import scala.collection.immutable.Range
import scala.xml.{NodeSeq, XML}

/**
 * Created by enrico on 5/21/15.
 */

class XMLRequirement(token: String, stags: String) extends SimpleRequirement(token,
  Tags.stringToTags(stags))

case class XMLProvides(name: String, value: Option[String])

case class XMLModule(name: String, factories: Set[XMLRuleFactory], rules: Set[XMLRule])

case class XMLProject(name: String, modules: Set[XMLModule]) {
  val factories = modules.flatMap(_.factories)
  val rules = modules.flatMap(_.rules)

  def solver(ui: UI) = new RuleSolver[String](factories.asInstanceOf[Set[RuleFactory[String]]],
    rules.asInstanceOf[Set[Rule[String]]], ui)
}

object XMLProject {

  import java.io.File
  private def recursiveListFiles(folder: File): Logged[Array[File]] = {
    val these = folder.listFiles
    if (these == null) {
      return "Error getting files from " + folder
    }

    val files = these.filter(_.isDirectory).foldLeft(Logged(Some(Array.empty[File]))){ (actual,ff) =>
      actual.fold(recursiveListFiles(ff))(_++_)
    }

    files.fold(these.filter{f => f.isFile && f.getName.endsWith(".rules.xml") })(_++_)
  }

  def apply(folder: File) : Logged[XMLProject] = {
    val files = recursiveListFiles(folder)
    files.value match {
      case Some(good) => apply(folder.getName, good.map{ f => (f.getName, new FileInputStream(f))}.toMap.asInstanceOf[Map[String,InputStream]])
      case _ => files.messages
    }
  }

  def apply(name: String, files: Map[String,InputStream]) : XMLProject = {
    val modules = files.map { case (moduleName,is) =>
      val rules = XML.load(is)

      val factories = (rules \ "factory").map { factory =>
        val name = (factory \ "@name").text

        val rules = getRules(factory)

        val create = (factory \ "create").map{ create =>
          create.text
        }

        XMLRuleFactory(name, rules.toSet, create.head)
      }

      XMLModule(moduleName, factories.toSet, getRules(rules).toSet)
    }
    new XMLProject(name, modules.toSet)
  }

  private def getRules(root: NodeSeq) = {
    (root \ "rule").map { rule =>
      val name = (rule \ "@name").text

      val tags = (rule \ "@tags").text

      val requirements = (rule \ "requires").map { requires =>
        val name = (requires \ "@name").text
        new XMLRequirement(name, "")
      }

      val provided = (rule \ "provides").map { provides =>
        val name = (provides \ "@name").text
        val value = provides.text
        if (value == null || value.isEmpty) {
          XMLProvides(name, None)
        } else {
          XMLProvides(name, Some(value))
        }
      }

      val run = (rule \ "run").map{ run =>
        run.text
      }

      if (run == null || run.isEmpty) {
        XMLRule(name, tags, requirements.toList, provided.toSet, None)
      } else {
        XMLRule(name, tags, requirements.toList, provided.toSet, Some(run.head))
      }
    }
  }
}

case class XMLRuleFactory(name: String, rules: Set[XMLRule], createScript: String) extends RuleFactory[String] {

  override def create(ui: UI): Set[Rule[String]] = {
    val mngr: ScriptEngineManager = new ScriptEngineManager

    val engine: ScriptEngine = mngr.getEngineByName("groovy")

    engine.put("ui", JavaUtils.toJavaUI(ui))

    val ruleName = engine.eval(createScript).asInstanceOf[String]

    val rulesMap = rules.map{ rule => (rule.name, rule) }.toMap

    if (rulesMap.contains(ruleName)) {
      Set(rulesMap(ruleName))
    } else {
      throw new RuntimeException()
    }
  }
}

case class XMLRule(name: String, tags: String, requiresList: List[Requirement[String]], xmlProvides: Set[XMLProvides],
                   runScript: Option[String]) extends Rule[String] {
  val requires = requiresList.toSet
  val provides = xmlProvides.map(_.name)
  override val providesTags = Tags.stringToTags(tags)

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    val mngr: ScriptEngineManager = new ScriptEngineManager

    val engine: ScriptEngine = mngr.getEngineByName("groovy")

    engine.put("ui", JavaUtils.toJavaUI(ui))

    Range(0, requiresList.size).foreach { i =>
      engine.put("r" + i, in(requiresList(i)))
    }
    val providedValues = xmlProvides.filter{ p => p.value.isDefined }

    val result = providedValues.map{ p =>
      val v = engine.eval("return (" + p.value.get + ")")
      (p.name, v)
    }.toMap

    runScript match {
      case Some(script) => result ++ JavaUtils.eval(engine, script)
      case _ => result
    }

  }

  override def toString = name
}
