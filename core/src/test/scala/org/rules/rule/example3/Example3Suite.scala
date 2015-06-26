package org.rules.rule.example3

import java.io.File

import org.rules.rule.xml.{XMLProjectFile, XMLRule, XMLProject}
import org.rules.rule.xml.XMLRule._
import org.rules.{UI, ConsoleUI}
import org.rules.rule._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite

/**
 * Created by enrico on 5/21/15.
 */
class Example3Suite extends FunSuite with MockFactory {

  test("test") {
    val ifProject = XMLProjectFile.create(new File("core/src/test/resources/org/rules/rule/example3"))

    if (ifProject.value.isEmpty) {
      fail(ifProject.messages.toString())
    }

    val project = ifProject.value.get.xmlProject

    val rules = project.rules

    assert(rules.nonEmpty)

    val connectionRule = rules.find(_.name == "Connection").get.toRule()

    val result = connectionRule.run(ConsoleUI,
      Map(
        SimpleRequirement("driverClassName") -> "oracle",
        SimpleRequirement("url") -> "oracleurl",
        SimpleRequirement("username") -> "oracleuser",
        SimpleRequirement("password") -> "oraclepwd"
      )
    )

    assert(result.contains("connection"))

    val connection = result("connection").asInstanceOf[java.util.Map[String,String]]

    assert(connection.get("driverClassName") == "oracle")
    assert(connection.get("url") == "oracleurl")
    assert(connection.get("username") == "oracleuser")
    assert(connection.get("password") == "oraclepwd")

    val oracleRule = rules.find(_.name == "Oracle").get.toRule()

    assert(oracleRule.providesTags == Tags(Map("dbType" -> "repo", "type" -> "cons")))

    val result1 = oracleRule.run(ConsoleUI,
      Map(
        SimpleRequirement("oracle.server") -> "oracle",
        SimpleRequirement("oracle.port") -> "oracleport",
        SimpleRequirement("oracle.sid") -> "oraclesid",
        SimpleRequirement("oracle.username") -> "oracleuser",
        SimpleRequirement("oracle.password") -> "oraclepwd"
      )
    )

    assert(result1("url") == "jdbc:oracle:thin:@oracle:oracleport:oraclesid")
    assert(result1("driverClassName") == "oracle.jdbc.driver.OracleDriver")
    assert(result1("username") == "oracleuser")
    assert(result1("password") == "oraclepwd")

    val ui = stub[UI]
    (ui.choose[String] _).when(*,*,*).returns(Some("Version1"))

    val factory = project.factories.head
    assert(factory.create(ui).head.toString == "Version1")
  }

}