package org.rules.rule

import java.io.File

import org.rules.SwingUI
import org.rules.rule.xml.XMLProject

/**
 * Created by enrico on 5/29/15.
 */
object Main {

  def main(args: Array[String]): Unit = {
    val project = XMLProject(new File((args(0))))
    project.value match {
      case Some(p) => p.solver(SwingUI).run()
      case _ => println(project.messages)
    }
  }

}
