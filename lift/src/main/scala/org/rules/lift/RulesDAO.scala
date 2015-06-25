package org.rules.lift

import java.io.File

/**
 * Created by enrico on 6/24/15.
 */
object RulesDAO {
  def projects = new File("data").listFiles().filter(_.isDirectory).sortWith(_.getName < _.getName)

  def addProject(name: String) = new File(new File("data"), name).mkdir()

  def delProject(name: String) = delete(new File(new File("data"), name))

  def delete(file: File) {
    if (file.isDirectory)
      Option(file.listFiles).map(_.toList).getOrElse(Nil).foreach(delete(_))
    file.delete
  }
}
