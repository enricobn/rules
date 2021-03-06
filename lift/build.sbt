/* start of xsbt-web-plugin configuration */
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"

//enablePlugins(WarPlugin)
enablePlugins(JettyPlugin)

// to force the version of jetty, jettyVersion is defined in Build.scala
containerLibs in Jetty := Seq("org.eclipse.jetty" % "jetty-runner" % jettyVersion intransitive())
// I think it works even without it, it seems that it's the default value
containerMain in Jetty := "org.eclipse.jetty.runner.Runner"
/* end of xsbt-web-plugin configuration */