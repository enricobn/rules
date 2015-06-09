package org.rules.rule

import java.io.{FileOutputStream, FileInputStream, File}

import org.rules.rule.xml.{XMLRule, XMLModule}
import org.scalatest.FunSuite

/**
 * Created by enrico on 6/6/15.
 */
class XMLSuite extends FunSuite {

  test("replace") {
    val tmp = File.createTempFile("xmlsuite", "rules.xml")
    println(tmp)
    new FileOutputStream(tmp).getChannel().transferFrom(
      new FileInputStream(new File("core/src/test/resources/org/rules/rule/example3/example3.rules.xml")).getChannel, 0, Long.MaxValue )
    val module = XMLModule(tmp)

    val rule =
        <rule name="Oracle" tags="dbType=repo,type=cons">
          <requires token="test"/>
          <provides token="test">'test'</provides>
          <run>
            <![CDATA[
            return ['test':'test']
            ]]>
          </run>

        </rule>

    val newModule = XMLModule.saveAndReload(module, XMLRule(rule))
    val oracle = newModule.rules.find(_.name == "Oracle").get
    assert(oracle.requires.head.token == "test")
  }

}