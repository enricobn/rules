package org.rules.rule

import org.rules.UI
import scala.collection.immutable.Map

object Tags {
  val empty = Tags(Map.empty)
  
  implicit def mapToTags(tags: Map[String,String]) : Tags = {
    Tags(tags)
  }

  implicit def stringToTags(tags: String) : Tags = {
    if (tags.isEmpty) {
      return Tags.empty
    }
    tags.split(",").map{ t =>
      val s = t.split("=")
      (s(0), s(1))
    }.toSet.toMap
  }
  
}

case class Tags(value: Map[String,String]) {
  def apply(key: String) = value(key)
  
  def ++(tags: Tags) : Tags = {
    Tags(value ++ tags.value)
  }
  
  def --(tags: Tags) = {
    Tags((value.toSet -- tags.value.toSet).toMap)
  }
  
  def contains(key: String) = value.contains(key)
  
  def matches(tags: Tags) : Boolean = {
    // I assume that if value does not contains a key for a given tag it means it's good for all values of
    // that tag
    tags.value.forall{ case (k,v) => !value.contains(k) || value(k) == v }
  }
  
//  def compatible(tags: Tags) : Boolean = {
//    
//  }
  
  def isEmpty : Boolean = value.isEmpty
 
}

trait Requirement[TOKEN] {
  def token: TOKEN
  def tags : Tags
}

trait Rule[TOKEN] {
  def requires : Set[Requirement[TOKEN]]
  def provides : Set[TOKEN]
  def providesTags : Tags = Tags.empty
  def run(ui: UI, in: Map[Requirement[TOKEN],AnyRef]) : Map[TOKEN,AnyRef]
//  def filter(in: Map[TOKEN,AnyRef]) : Boolean = true
  val cache: Boolean = true
  
  override def toString = {
    getClass.getName
  }
}

trait RuleFactory[TOKEN] {
  def create(ui: UI) : Set[Rule[TOKEN]]
}
