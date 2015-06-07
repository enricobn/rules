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

