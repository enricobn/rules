package org.rules.rule

import scala.runtime.ScalaRunTime

case class SimpleRequirement(token: String, tags: Tags = Tags.empty) extends Requirement[String] {

  override def toString : String = {
    if (tags.isEmpty) {
      token.toString()
    } else {
      ScalaRunTime._toString(this)
    }
  }

}