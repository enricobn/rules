package org.rules.lift.utils

import net.liftweb.json.{Serialization, DefaultFormats}
import net.liftweb.json.JsonAST.JValue

/**
 * Created by enrico on 7/13/15.
 */
trait LiftList[T] {
  def toJson(value: T): JValue
  def fromJson(value: JValue): T

  protected def write(value: T) : String = {
    implicit val formats = DefaultFormats
    Serialization.write(toJson(value))
  }
}
