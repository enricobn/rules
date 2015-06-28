package org.rules.rule

import java.io.{FileOutputStream, FileInputStream, File}

import org.rules.rule.xml.{XMLModuleFile, XMLRule, XMLModule}
import org.scalatest.FunSuite

/**
 * Created by enrico on 6/6/15.
 */
class XMLSuite extends FunSuite {

  test("updateAndSave") {
    val tmp = File.createTempFile("xmlsuite", "rules.xml")
    println(tmp)
    new FileOutputStream(tmp).getChannel().transferFrom(
      new FileInputStream(new File("core/src/test/resources/org/rules/rule/example3/example3.rules.xml")).getChannel, 0, Long.MaxValue )
    val module = XMLModuleFile(/*"1",*/ tmp)

    val rule =
        <rule id="2" name="Oracle" tags="dbType=repo,type=cons">
          <requires token="test"/>
          <provides token="test">'test'</provides>
          <run>
            <![CDATA[
            return ['test':'test']
            ]]>
          </run>

        </rule>

    val newModule = module.updateAndSave(Seq(XMLRule(rule))).xmlModule
    val oracle = newModule.rules.find(_.id == "2").get
    assert(oracle.requires.head.token == "test")
  }

  test("save new line") {
    val tmp = File.createTempFile("xmlsuite", "rules.xml")
    println(tmp)
    new FileOutputStream(tmp).getChannel().transferFrom(
      new FileInputStream(new File("core/src/test/resources/org/rules/rule/example3/example3.rules.xml")).getChannel, 0, Long.MaxValue )
    val module = XMLModuleFile(/*"1",*/ tmp)

    val rule =
      <rule id="2" name="Oracle" tags="dbType=repo,type=cons">
        <requires token="test"/>
        <provides token="test">'test'</provides>
        <run>
          <![CDATA[
          return ['test':'test']
          ]]>
        </run>

      </rule>

    module.updateAndSave(Seq(XMLRule(rule)))

    val newModule = XMLModuleFile(/*"1",*/ tmp).xmlModule

    val oracle = newModule.rules.find(_.id == "2").get
    assert(oracle.run.contains("\n"))
  }

  test("save new line 2") {
    val tmp = File.createTempFile("xmlsuite", "rules.xml")
    println(tmp)
    new FileOutputStream(tmp).getChannel().transferFrom(
      new FileInputStream(new File("core/src/test/resources/org/rules/rule/example3/example3.rules.xml")).getChannel, 0, Long.MaxValue )
    val module = XMLModuleFile(/*"1",*/ tmp)

    val rule = new XMLRule("2", "Oracle", "", Seq.empty, Seq.empty, "pippo\npluto")

    module.updateAndSave(Seq(rule))

    val newModule = XMLModuleFile(/*"1",*/ tmp).xmlModule
    val oracle = newModule.rules.find(_.id == "2").get
    assert(oracle.run.contains("\n"))
  }


  test("update") {
    val module = XMLModule("test", <rules></rules>)

    val rule =
      <rule id="2" name="Oracle" tags="dbType=repo,type=cons">
        <requires token="test"/>
        <provides token="test">'test'</provides>
        <run>
          <![CDATA[
            return ['test':'test']
            ]]>
        </run>

      </rule>

    val newModule = module.update(Seq(XMLRule(rule)))
    val oracle = newModule.rules.find(_.id == "2").get
    assert(oracle.requires.head.token == "test")
  }

  test("update 2") {
    val module = XMLModule("test",
      <rules>
        <rule id="2" name="Oracle" tags="dbType=repo,type=cons">
          <requires token="test"/>
          <provides token="test">'test'</provides>
          <run>
            <![CDATA[
              return ['test':'test']
            ]]>
          </run>
        </rule>
      </rules>)

    val newModule = module.update(Seq.empty[XMLRule])
    val oracle = newModule.rules.find(_.id == "2").get
    assert(oracle.requires.head.token == "test")
  }

}
