import sbt._
import Keys._

object BuildSettings {
  val scala = "2.11.1"

  val bsDefault = Defaults.defaultSettings ++ Seq(
    organization  := "org.rules"
  , scalaVersion  := scala
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
  val liftVersion = "2.6"

  val jetty = "org.eclipse.jetty" % "jetty-webapp" % "8.1.7.v20120910"  %
      "container,test"
  val orbit = "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" %
      "container,compile" artifacts Artifact("javax.servlet", "jar", "jar")
//  val liftWebKit = "net.liftweb" %% "lift-webkit" % liftVersion % "compile"


  val liftweb = Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-mapper" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-wizard" % liftVersion % "compile->default")


  val logback = "ch.qos.logback" % "logback-classic" % "1.0.13"

  val scalamock = "org.scalamock" % "scalamock-scalatest-support_2.11" % "3.2.2" % "test"

  val typesafe_config = "com.typesafe" % "config" % "1.2.1"

  val scala_swing =
    CrossVersion.partialVersion(BuildSettings.scala) match {
      // if scala 2.11+ is used, add dependency on scala-xml module
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        Seq(
          "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
          "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
          "org.scala-lang.modules" %% "scala-swing" % "1.0.1")
      case _ =>
        // or just libraryDependencies.value if you don't depend on scala-swing
        Seq("org.scala-lang" % "scala-swing" % BuildSettings.scala)
    }

  val groovy = "org.codehaus.groovy" % "groovy-all" % "2.4.3"
  //val scalaTest = "org.scalatest" %% "scalatest" % "2.0" % "test"
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
  autoScalaLibrary := false
  dependencyOverrides += "org.scala-lang" % "scala-library" % scala

  //javaOptions += "-XX:MaxPermSize=512m"

  //fork in run := true

  //javaHome := sys.env.get("JAVA_HOME") map file

  val jv = sys.props("java.specification.version")
  if (jv != "1.7") {
    throw new RuntimeException("Expected java version 1.7 but was " + jv +
      " set an appropriate JAVA_HOME environment variable.")
  }

  val scalaBuildOptions = Seq("-target:jvm-1.7")

  val depsCore = Seq(
    scalamock,
    typesafe_config,
    groovy
  ) ++ scala_swing

  val depsLift = Seq(
    scalamock,
    jetty
  , orbit
  , logback
  ) ++ liftweb

  lazy val core = Project(
    "core",
    file("core"),
    settings = bsCore ++ Seq(
      libraryDependencies := depsCore,
      scalacOptions ++= scalaBuildOptions
    )
  )

  lazy val lift = Project(
    "lift",
    file("lift"),
    settings = bsLift ++ Seq(
      libraryDependencies := depsLift
    ) ++ webSettings  ++ Seq(
      artifactName in packageWar :=
        ((_: ScalaVersion, _: ModuleID, _: Artifact) => "rules.war"),
      port in container.Configuration := 8071,
      scanDirectories in Compile := Nil,
      scalacOptions ++= scalaBuildOptions
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

