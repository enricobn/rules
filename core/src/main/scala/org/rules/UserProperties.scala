package org.rules
import com.typesafe.config._
import java.io.File

object UserProperties {
  // TODO
  val conf = ConfigFactory.parseFile(new File("core/src/test/resources/org/rules/example.properties"))
  
  def get(key: String) : String = {
    conf.getString(key)
  }
}