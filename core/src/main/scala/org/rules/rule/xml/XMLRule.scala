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
import scala.xml.{Elem, Node, NodeSeq, XML}

/**
 * Created by enrico on 5/21/15.
 */

class XMLRequirement(token: String, stags: String) extends SimpleRequirement(token,
  Tags.stringToTags(stags))

case class XMLProvides(name: String, value: Option[String])

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
    <rule name={name} tags={tags}>
      {for (requirement <- requiresList) yield
       <requires name={requirement.token}></requires>
      }

      {for (provider <- xmlProvides) yield
        <provides name={provider.name}>{provider.value}</provides>
      }

      {runScript match {
        case Some(script) if script.nonEmpty => <run>{scala.xml.PCData(script)}</run>
        case _ =>
        }
      }
    </rule>
  }

  override def toString = name
}
