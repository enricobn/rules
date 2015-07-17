package org.rules.rule.xml

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.script.{ScriptEngine, ScriptEngineManager}

import org.rules.rule._
import org.rules.utils.Utils
import org.rules.{JavaUtils, UI}

import scala.collection.immutable.Range
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml._

/**
 * Created by enrico on 5/21/15.
 */

object XMLRequirement {
  implicit def toRequirement(xmlRequirement: XMLRequirement): Requirement[String] =
    SimpleRequirement(xmlRequirement.token, Tags.stringToTags(xmlRequirement.tags))
}

case class XMLRequirement(token: String, tags: String)

case class XMLProvides(token: String, value: String)

object XMLRule {

  def apply(xml: Node) : XMLRule = {
    val id = (xml \ "@id").text

    if (id == null || id.isEmpty) {
      throw new IllegalArgumentException("id is required.")
    }

    val name = (xml \ "@name").text

    if (name == null || name.isEmpty) {
      throw new IllegalArgumentException("name is required.")
    }

    val tags = (xml \ "@tags").text

    val requirements = (xml \ "requires").map { requires =>
      val token = (requires \ "@token").text
      val tags = (requires \ "@tags").text
      new XMLRequirement(token, tags)
    }

    val provided = (xml \ "provides").map { provides =>
      val token = (provides \ "@token").text
      val value = provides.text
      XMLProvides(token, value)
    }

    val run = (xml \ "run").map{ run =>
      run.text
    }

    if (run == null || run.isEmpty) {
      XMLRule(id, name, tags, requirements.toList, provided.toList, "")
    } else {
      XMLRule(id, name, tags, requirements.toList, provided.toList, run.head)
    }
  }
}

case class XMLRule(id: String, name: String, tags: String, requires: Seq[XMLRequirement], provides: Seq[XMLProvides],
                   run: String) {

  def toRule() : Rule[String] = {
    new Rule[String] {
      val requires = XMLRule.this.requires.map(XMLRequirement.toRequirement(_)).toSet
      val provides = XMLRule.this.provides.map(_.token).toSet
      override val providesTags = Tags.stringToTags(tags)

      def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
        val mngr: ScriptEngineManager = new ScriptEngineManager

        val engine: ScriptEngine = mngr.getEngineByName("groovy")

        engine.put("ui", JavaUtils.toJavaUI(ui))

        Range(0, XMLRule.this.requires.size).foreach { i =>
          engine.put("r" + i, in(XMLRule.this.requires(i)))
        }
        val providedValues = XMLRule.this.provides.filter{ p => !p.value.isEmpty }

        val result = providedValues.map{ p =>
          val v = engine.eval("return (" + p.value + ")")
          (p.token, v)
        }.toMap

        if (!XMLRule.this.run.isEmpty) {
          result ++ JavaUtils.eval(engine, XMLRule.this.run)
        } else {
          result
        }
      }

      override def toString = XMLRule.this.name
    }
  }

  /*
  override val requires = requiresList.map(XMLRequirement.toRequirement(_)).toSet
  val provides = xmlProvides.map(_.name).toSet
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
*/

  def toXML() : NodeSeq = {
    <rule id={id} name={name} tags={tags}>
      {for (requirement <- requires) yield
       <requires token={requirement.token} tags={requirement.tags.toString}></requires>
      }
      {for (provider <- provides) yield
        <provides token={provider.token}>{provider.value}</provides>
      }
      {if (!run.isEmpty) <run>{PCData(run)}</run>}
    </rule>
  }

}
