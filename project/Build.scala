import sbt._
import Keys._

object BuildSettings {
  val bsDefault = Defaults.defaultSettings ++ Seq(
    organization  := "org.rules"
  , scalaVersion  := "2.11.6"
  , scalacOptions := Seq("-unchecked", "-deprecation", "-Yrepl-sync", "-encoding", "UTF-8", "-optimise")
  )

  val bsCore = bsDefault ++ Seq(
    name          := "rules-core"
  , version       := "0.1-SNAPSHOT"
  ) 

  val bsLift = bsDefault ++ Seq(
    name          := "rules-lift"
  , version       := "0.1-SNAPSHOT"
  )
}

object Dependencies {
  val liftVersion = "2.6-RC1"

  val jetty = "org.eclipse.jetty" % "jetty-webapp" % "8.1.7.v20120910"  %
      "container,test"
  val orbit = "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" %
      "container,compile" artifacts Artifact("javax.servlet", "jar", "jar")
  val liftWebKit = "net.liftweb" %% "lift-webkit" % liftVersion % "compile"

  val logback = "ch.qos.logback" % "logback-classic" % "1.0.13"

//  val scalaTest = "org.scalatest" %% "scalatest" % "2.0" % "test"
}

object RulesBuild extends Build {
  import BuildSettings._
  import Dependencies._

  // Web plugin
  import com.earldouglas.xsbtwebplugin.WebPlugin._
  import com.earldouglas.xsbtwebplugin.PluginKeys._

  // Coffeescript plugin
//  import coffeescript.Plugin._
//  import CoffeeKeys._

  // Less plugin
//  import less.Plugin._
//  import LessKeys._

  val depsCore = Seq(
//    scalaTest
  )

  val depsLift = Seq(
    jetty
  , orbit
  , liftWebKit
  , logback
  )

  lazy val core = Project(
    "core",
    file("core"),
    settings = bsCore ++ Seq(
      libraryDependencies := depsCore
    )
  )

  lazy val lift = Project(
    "lift",
    file("lift"),
    settings = bsLift ++ Seq(
      libraryDependencies := depsLift
    ) ++ webSettings  ++ Seq(
      artifactName in packageWar :=
        ((_: ScalaVersion, _: ModuleID, _: Artifact) => "rules.war")
    , port in container.Configuration := 8071
    , scanDirectories in Compile := Nil,
    javaOptions += "-XX:MaxPermSize=512m"

  )
/*
    ++ coffeeSettings ++ Seq(
      resourceManaged in (Compile, coffee) <<= (webappResources in Compile)(_.get.head / "static" / "coffee")
    ) ++ lessSettings ++ Seq(
      mini in (Compile, less) := true
    , resourceManaged in (Compile, less) <<= (webappResources in Compile)(_.get.head / "static" / "less")
    )
*/
  ) dependsOn(core)
}

