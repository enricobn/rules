package org.rules.rule.xml

import javax.script.{ScriptEngine, ScriptEngineManager}

import org.rules.rule.{RuleFactory, Rule}
import org.rules.{UI, JavaUtils}

/**
 * Created by enrico on 6/7/15.
 */
case class XMLRuleFactory(name: String, rules: Set[XMLRule], createScript: String) extends RuleFactory[String] {

   override def create(ui: UI): Set[Rule[String]] = {
     val mngr: ScriptEngineManager = new ScriptEngineManager

     val engine: ScriptEngine = mngr.getEngineByName("groovy")

     engine.put("ui", JavaUtils.toJavaUI(ui))

     val ruleName = engine.eval(createScript).asInstanceOf[String]

     val rulesMap = rules.map{ rule => (rule.name, rule.toRule()) }.toMap

     if (rulesMap.contains(ruleName)) {
       Set(rulesMap(ruleName))
     } else {
       throw new RuntimeException()
     }
   }
 }
