package org.rules.rule.example2

import org.rules.UI
import org.rules.rule.{SimpleRequirement, Rule, Requirement}

object Main extends Rule[String] {
  def requires = Set.empty
  def provides = Set("url")
  override def providesTags = Map("dbType" -> "main", "type" -> "dev")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("url" -> "mainURL")
  }
  
}

object Main1 extends Rule[String] {
  val repoDev = SimpleRequirement("connection", Map("dbType" -> "main", "type" -> "dev"))
  def requires = Set(repoDev)
  def provides = Set("url")
  override def providesTags = Map("dbType" -> "main", "type" -> "cons")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    println(in(repoDev))
    Map("url" -> "main1URL")
  }
  
}

object Repo extends Rule[String] {
  def requires = Set.empty
  def provides = Set("url")
  override def providesTags = Map("dbType" -> "repo", "type" -> "dev")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("url" -> "repoURL")
  }
  
}

object Repo1 extends Rule[String] {
  def requires = Set.empty
  def provides = Set("url")
  override def providesTags = Map("dbType" -> "repo", "type" -> "cons")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("url" -> "repo1URL")
  }
  
}

object Connection extends Rule[String] {
  def requires = Set("url")
  def provides = Set("connection")
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("connection" -> ("connection to " + in("url")))
  }
  
}

object Goal extends Rule[String] {
  val main = SimpleRequirement("connection", Map("dbType" -> "main", "type" -> "cons"))
  val repo = SimpleRequirement("connection", Map("dbType" -> "repo", "type" -> "cons"))

  def requires = Set(main, repo)
  def provides = Set.empty
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    println(in(main))
    println(in(repo))
    Map.empty
  }
}
