package org.rules.lift.utils

import net.liftweb.json.{Serialization, DefaultFormats}
import net.liftweb.json.JsonAST.JValue

/**
 * Created by enrico on 7/13/15.
 */
trait LiftListView[T] {
  def toJson(item: T): JValue
  def fromJson(jsonItem: JValue): T

  protected def write(item: T) : String = {
    write(toJson(item))
  }

  protected def write(jsonItem: JValue) : String = {
    implicit val formats = DefaultFormats
    Serialization.write(jsonItem)
  }

}
