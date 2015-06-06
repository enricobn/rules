package org.rules.rule

import java.io.{FileOutputStream, FileInputStream, File}

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
      <rules>
        <rule name="Oracle" tags="dbType=repo,type=cons">
          <requires name="test"/>
          <provides name="test">'test'</provides>
          <run>
            <![CDATA[
            return ['test':'test']
            ]]>
          </run>

        </rule>
      </rules>

    val newModule = XMLModule.saveAndReload(module, XMLModule.getRules(rule).head)
    val oracle = newModule.rules.find(_.name == "Oracle").get
    assert(oracle.requires.head.token == "test")
  }

}
