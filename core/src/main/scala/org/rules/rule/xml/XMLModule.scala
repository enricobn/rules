package org.rules.rule.xml

import java.io.{FileInputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.xml.{NodeSeq, Elem, Node, XML}
import scala.xml.transform.{RuleTransformer, RewriteRule}

/**
 * Created by enrico on 6/7/15.
 */
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
         case sn@Elem(_, "rule", _, _, _*) if sn.attribute("id").get.head.text == rule.id => rule.toXML()
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

 }

case class XMLModule(file: File) {
  val name = file.getName

  val xml = XML.load(new FileInputStream(file))

  val rules = (xml \ "rule").map(XMLRule(_))

  findDuplicates(rules)

  val factories = (xml \ "factory").map { factory =>
    val name = (factory \ "@name").text
    val rules = (factory \ "rule").map(XMLRule(_))
    findDuplicates(rules)
    val create = (factory \ "create").map(_.text)

    XMLRuleFactory(name, rules.toSet, create.head)
  }

  private def findDuplicates(rules: Seq[XMLRule]) =
    rules.groupBy(_.id).find(_._2.size > 1) match {
      case Some(x) => throw new IllegalStateException("duplicated id=" + x._2.head.id)
      case _ =>
    }
}

