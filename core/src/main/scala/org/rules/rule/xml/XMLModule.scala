package org.rules.rule.xml

import java.io.{FileInputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.xml.{NodeSeq, Elem, Node, XML}
import scala.xml.transform.{RuleTransformer, RewriteRule}

/**
 * Created by enrico on 6/7/15.
 */

case class XMLModuleFile(id: String, file: File) {
  private val name = file.getName().substring(0,file.getName.length - 10)

  lazy val xmlModule = XMLModule(name, XML.load(new FileInputStream(file)))

  def updateAndSave(changedRules: Seq[XMLRule]) : XMLModuleFile = {
    val updated = xmlModule.update(changedRules)
    updated.save(file)

    new XMLModuleFile(id, file) {
      // since I don't want to reload the file!
      override lazy val xmlModule = updated
    }
  }

}

object XMLModule {

  def findDuplicates(rules: Seq[XMLRule]) =
    rules.groupBy(_.id).find(_._2.size > 1) match {
      case Some(x) => throw new IllegalStateException("duplicated id=" + x._2.head.id)
      case _ =>
    }

  def apply(name: String, xml: NodeSeq) : XMLModule = {
    val rules = (xml \ "rule").map(XMLRule(_))

    XMLModule.findDuplicates(rules)

    val factories = (xml \ "factory").map { factory =>
      val name = (factory \ "@name").text
      val rules = (factory \ "rule").map(XMLRule(_))
      XMLModule.findDuplicates(rules)
      val create = (factory \ "create").map(_.text)

      XMLRuleFactory(name, rules.toSet, create.head)
    }

    new XMLModule(name, rules, factories) {
      override def toXML() = xml
    }
  }
}

case class XMLModule(name: String, rules: Seq[XMLRule], factories: Seq[XMLRuleFactory]) {
  def toXML() : NodeSeq =
    <rules>
      {for (rule <- rules) yield
        rule.toXML()
      }
      {for (factory <- factories) yield
        factory.toXML()
      }
    </rules>

  /**
   * if exists replace the rule in the module
   * @param changedRules to replace
   * @return
   */
  def update(changedRules: Seq[XMLRule]) : XMLModule = {

    val updatedRules = rules.map{ rule =>
      if (changedRules.exists(_.id == rule.id)) {
        changedRules.find(_.id == rule.id).get
      } else {
        rule
      }
    }

    val addedRules = changedRules.filter{ rule => !rules.exists(_.id == rule.id)}

    XMLModule(name, updatedRules ++ addedRules, factories)

    /*
    // TODO add not existent module
    object t extends RewriteRule {
      private def containsRule(id: String) = {
        changedRules.exists(_.id == id)
      }

      override def transform(n: Node): Seq[Node] = n match {
        case rulesE@Elem(_, "rules", _, _, _*) =>
          <rules>{
            (rulesE.descendant \ "rule").map{ rule =>
              val id = rule.attribute("id").get.head.text
              if (containsRule(id)) {
                changedRules.find(_.id == id).get.toXML()
              } else {
                rule
              }
            }
            }</rules>
        case other => other
      }
    }

    XMLModule(name, new RuleTransformer(t).transform(toXML()).head)
    */
  }

  def save(file: File) = {
/* commented since it removes new lines from CDATA, but I need them for scripts
    val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)
    val prettyXml = prettyPrinter.format(xml)
    */

    Files.write(file.toPath, toXML().head.toString().getBytes(StandardCharsets.UTF_8))
  }
}

