package org.rules.lift.snippet

import net.liftweb.json._
import org.rules.rule.xml.XMLRule
import org.scalatest.FunSuite

/**
 * Created by enrico on 6/9/15.
 */
class EditRulesSuite extends FunSuite {

    test("json") {
      val xml =
        <rule name="test" tags="dbType=main">
          <requires token="req1" tags="db=dev"></requires>
          <requires token="req2" tags="db=dev"></requires>
          <provides token="pr1">r0 + r1</provides>
          <run>
            <![CDATA[
            return ['test':'test']
            ]]>
          </run>
        </rule>

      val rule = XMLRule(xml)

      val json = EditRules.ruleToJson(rule)

      val fromJson = EditRules.jsonToRule(json)

      assert(rule == fromJson)
    }
}
