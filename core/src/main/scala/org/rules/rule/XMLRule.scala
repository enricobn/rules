package org.rules.rule

import java.io.{FileOutputStream, File, FileInputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.print.DocFlavor.INPUT_STREAM
import javax.script.{ScriptEngine, ScriptEngineManager}

import org.rules.{JavaUtils, UI}

import scala.collection.generic.IsTraversableLike
import scala.collection.immutable.Range
import scala.xml.transform.{RuleTransformer, RewriteRule}
import scala.xml.{Elem, Node, NodeSeq, XML}

/**
 * Created by enrico on 5/21/15.
 */

class XMLRequirement(token: String, stags: String) extends SimpleRequirement(token,
  Tags.stringToTags(stags))

case class XMLProvides(name: String, value: Option[String])

object XMLModule {

  /**
   * if exists replace the rule in the module, otherwise adds it, then save the module and reloads it
   * @param module with rule
   * @param rule to replace
   * @return
   */
  def saveAndReload(module: XMLModule, rule: XMLRule) : XMLModule = {
    // TODO add not existent module
    object t extends RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case sn@Elem(_, "rule", _, _, _*) if sn.attribute("name").get.head.text == rule.name => rule.toXML()
        case other => other
      }
    }

    val transformed = new RuleTransformer(t).transform(module.xml).head

    val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)
    val prettyXml = prettyPrinter.format(transformed)

    Files.write(module.file.toPath, prettyXml.getBytes(StandardCharsets.UTF_8))

//    XML.save(module.file.getAbsolutePath, new RuleTransformer(t).transform(module.xml).head)
    XMLModule(module.file)
  }

  def getRules(root: NodeSeq) = {
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

case class XMLModule(file: File) {
  val name = file.getName

  val xml = XML.load(new FileInputStream(file))

  val rules = XMLModule.getRules(xml)

  val factories = (xml \ "factory").map { factory =>
    val name = (factory \ "@name").text

    val rules = XMLModule.getRules(factory)

    val create = (factory \ "create").map{ create =>
      create.text
    }

    XMLRuleFactory(name, rules.toSet, create.head)
  }
}

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
      case Some(good) => apply(folder.getName, good)
      case _ => files.messages
    }
  }

  def apply(name: String, files: Seq[File]) : XMLProject = {
    val modules = files.map (XMLModule(_))
    new XMLProject(name, modules.toSet)
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

  def toXML() : NodeSeq = {
    <rule name={name} tags={tags}>{
      for (requirement <- requiresList) yield
       <requires name={requirement.token}></requires>
      }{for (provider <- xmlProvides) yield
        <provides name={provider.name}>{provider.value}</provides>
      }{runScript match {
        case Some(script) if script.nonEmpty => <run>{scala.xml.PCData(script)}</run>
        case _ =>
        }
      }</rule>
  }

  override def toString = name
}
