package org.rules.rule

import org.rules.{UI, UserProperties, SwingUI}
import scala.runtime.ScalaRunTime

object Oracle extends Rule[String] {
  
  def requires = Set("oracle.server", "oracle.port", "oracle.sid", "oracle.username", "oracle.password")
  
  def provides = Set("url", "driverClassName", "username", "password")
  
  override def providesTags = Map("db" -> "Oracle")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map(
        "url" -> ("jdbc:oracle:thin:@" + in("oracle.server") + ":" 
          + in("oracle.port") + ":" + in("oracle.sid")),
       "driverClassName" -> "oracle.jdbc.driver.OracleDriver",
       "username" -> in("oracle.username"),
       "password" -> in("oracle.password")
    )
  }
}

object OracleByCatalog extends Rule[String] {
    
  def requires = Set("catalog")
        
  def provides = Set("oracle.username", "oracle.password")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
      Map("oracle.username" -> in("catalog"), "oracle.password" -> in("catalog"))
  }
}

object OracleConnectionFunction extends Rule[String] {

  def requires = Set("oracle.server", "oracle.port", "oracle.sid")

  def provides = Set("connectionFunction")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("connectionFunction" -> { (catalog : String ) =>
      Map(
        "url" -> ("jdbc:oracle:thin:@" + in("oracle.server") + ":"
          + in("oracle.port") + ":" + in("oracle.sid")),
        "driverClassName" -> "oracle.jdbc.driver.OracleDriver",
        "username" -> catalog,
        "password" -> catalog
      )

    })
  }
}

object SQLServer extends Rule[String] {
  
  def requires = Set("sqlserver.server", "sqlserver.port", "sqlserver.database", "sqlserver.username", 
      "sqlserver.password")
  
  def provides = Set("url", "driverClassName", "username", "password")
  
  override def providesTags = Map("db" -> "SQLServer")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map(
        "url" -> ("jdbc:jtds:sqlserver://" + in("sqlserver.server") + ":" + in("sqlserver.port") + "/" 
            + in("sqlserver.database")),
        "driverClassName" -> "net.sourceforge.jtds.jdbc.Driver",
        "username" -> in("sqlserver.username"),
        "password" -> in("sqlserver.password")
    )
  }
  
}

object SQLServerConnectionFunction extends Rule[String] {

  def requires = Set("sqlserver.server", "sqlserver.port", "sqlserver.username", "sqlserver.password")

  def provides = Set("connectionFunction")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("connectionFunction" -> { (catalog : String ) =>
      Map(
        "url" -> ("jdbc:jtds:sqlserver://" + in("sqlserver.server") + ":" + in("sqlserver.port") + "/"
          + catalog),
        "driverClassName" -> "net.sourceforge.jtds.jdbc.Driver",
        "username" -> in("sqlserver.username"),
        "password" -> in("sqlserver.password")
      )
    })
  }
}

object SQLServerByCatalog extends Rule[String] {
  
  def requires = Set("catalog")
  
  def provides = Set("sqlserver.database")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("sqlserver.database" -> in("catalog"))
  }
  
}

object RepositoryDevCatalog extends Rule[String] {

  def requires = Set("version")
  
  def provides = Set("catalog")
  
  override def providesTags = Map("dbType" -> "repo", "type" -> "dev")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("catalog" -> UserProperties.get("repository.dev.catalog." + in("version")),
        "repo" -> "repo")
  }
}

object MainDevCatalog extends Rule[String] {
  
  def requires = Set("version")
  
  def provides = Set("catalog")
  
  override def providesTags = Map("dbType" -> "main", "type" -> "dev")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("catalog" -> UserProperties.get("main.dev.catalog." + in("version")),
        "main" -> "main")
  }
}

object SQLServerDev extends Rule[String] {
  
  def requires = Set("version")
  
  def provides = Set("sqlserver.username", "sqlserver.password", "sqlserver.server", "sqlserver.port")
  
  override def providesTags = Map("type" -> "dev")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map(
        "sqlserver.username" -> "devuser",
        "sqlserver.password" -> "devpassword",
        "sqlserver.server" -> UserProperties.get("dev.server." + in("version")),
        // TODO it's an int
        "sqlserver.port" -> "1421"
    )
  }
}

object OracleDev extends Rule[String] {
  def requires = Set("version")
  
  def provides = Set("oracle.server", "oracle.port", "oracle.sid")
  
  override def providesTags = Map("type" -> "dev")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map(
        "oracle.server" -> UserProperties.get("dev.oracle.server." + in("version")),
        "oracle.sid" -> UserProperties.get("dev.oracle.sid." + in("version")),
        "oracle.port" -> "1521"
    )
  }
}

object ConnectionRule extends Rule[String] {
  
  def requires = Set("driverClassName", "url", "username", "password")
  
  def provides = Set("connection")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    // TODO do a mapFromContext utility
    Map("connection" -> Map(
        "driverClassName" -> in("driverClassName"),
        "url" -> in("url"),
        "username" -> in("username"),
        "password" -> in("password")
      )
    )
  }
}

object DummyVersionRule extends Rule[String] {
  
  def requires = Set.empty
  
  def provides = Set("version")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    ui.choose("Choose", "Version", List("4.3.2","5.0.1")) match {
      case Some(version) => Map("version" -> version)
      case None => Map.empty
    }
  }
}

object SQLServerCons2005 extends Rule[String] {
  def requires = Set()
  def provides = Set("sqlserver.username", "sqlserver.password", "sqlserver.server", "sqlserver.port")
  override def providesTags = Map("dbType" -> "main", "type" -> "cons", "dbCons" -> "2005")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("sqlserver.username" -> "2005.username",
        "sqlserver.password" -> "2005.password",
        "sqlserver.server" -> "sql2005cons",
        "sqlserver.port" -> "1421")
  }
}

object SQLServerCons2008 extends Rule[String] {
  def requires = Set()
  def provides = Set("sqlserver.username", "sqlserver.password", "sqlserver.server", "sqlserver.port")
  override def providesTags = Map("dbType" -> "main", "type" -> "cons", "dbCons" -> "2008")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("sqlserver.username" -> "2008.username",
        "sqlserver.password" -> "2008.password",
        "sqlserver.server" -> "sql2008cons",
        "sqlserver.port" -> "1421")
  }
}

object RepositoryCons extends Rule[String] {
  def requires = Set("version")
  def provides = Set("catalog", "sqlserver.username", "sqlserver.password", "sqlserver.server", "sqlserver.port")
  override def providesTags = Map("dbType" -> "repo", "type" -> "cons")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    val version = in("version")
    if (version == "4.3.2") {
      Map(
        "catalog" -> "TCPM_REPOS_432",
        "sqlserver.username" -> "2008.username",
        "sqlserver.password" -> "2008.password",
        "sqlserver.server" -> "sql2008cons",
        "sqlserver.port" -> "1421"
      )
    } else if (version == "5.0.1") {
      Map(
        "catalog" -> "TCPM_REPOS_501",
        "sqlserver.username" -> "2005.username",
        "sqlserver.password" -> "2005.password",
        "sqlserver.server" -> "sql2005cons",
        "sqlserver.port" -> "1421"
      )
    } else {
      Map.empty
    }
  }
}

object MainCons extends Rule[String] {
  private val repo = SimpleRequirement("connection", Map("dbType" -> "repo", "type" -> "cons"))
  private val connectionFunction2005 = SimpleRequirement("connectionFunction",
    Map("dbType" -> "main", "type" -> "cons", "dbCons" -> "2005"))
  private val connectionFunction2008 = SimpleRequirement("connectionFunction",
    Map("dbType" -> "main", "type" -> "cons", "dbCons" -> "2008"))
  def requires = Set(repo, connectionFunction2005, connectionFunction2008)
  def provides = Set("connection")
  override def providesTags = Map("dbType" -> "main", "type" -> "cons")
  
  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    ui.choose("Choose", "DB", List("CONS_DB2005","CONS_DB2008")) match {
      case Some(db) =>
        val connectionFunction = (if (db == "CONS_DB2005") in(connectionFunction2005) else in(connectionFunction2008))
          .asInstanceOf[String => Map[String,String]]
        Map("connection" ->  connectionFunction(db))
      case None => Map.empty
    }
  }
}

object GoalDev extends Rule[String] {
  private val main = SimpleRequirement("connection", Map("dbType" -> "main", "type" -> "dev"))
  private val repo = SimpleRequirement("connection", Map("dbType" -> "repo", "type" -> "dev"))

  def requires = Set(main,repo)
  def provides = Set("connectionRepo", "connectionMain")
  override def providesTags = Map("type" -> "dev")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("connectionRepo" -> in(repo), "connectionMain" -> in(main))
  }
}

object GoalCons extends Rule[String] {
  private val main = SimpleRequirement("connection", Map("dbType" -> "main", "type" -> "cons"))
  private val repo = SimpleRequirement("connection", Map("dbType" -> "repo", "type" -> "cons"))

  def requires = Set(main,repo)
  def provides = Set("connectionRepo", "connectionMain")
  override def providesTags = Map("type" -> "cons")

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    Map("connectionRepo" -> in(repo), "connectionMain" -> in(main))
  }
}

object GoalFactory extends RuleFactory[String] {

  override def create(ui: UI): Set[Rule[String]] = {
    ui.choose("Choose", "Type", List("Cons", "Dev")) match {
      case Some(t) => Set(if (t == "Cons") GoalCons else GoalDev)
      case _ => throw new RuntimeException()
    }
  }
}

object Goal extends Rule[String] {
//  private val main = SimpleRequirement("connection", Map("dbType" -> "main", "type" -> connectionType))
//  private val repo = SimpleRequirement("connection", Map("dbType" -> "repo", "type" -> connectionType))
  private val repoDev = SimpleRequirement("connection", Map("dbType" -> "repo", "type" -> "dev"))
  
  def requires = Set("connectionMain","connectionRepo",repoDev)

  def provides = Set.empty

  def run(ui: UI, in: Map[Requirement[String],AnyRef]) : Map[String,AnyRef] = {
    def mainConnection = in("connectionMain")
    def repoConnection = in("connectionRepo")
    def repoDevConnection = in(repoDev)
    println("Goal main=" + mainConnection + " repository=" + repoConnection + " repoDev=" + repoDevConnection)
    Map()
  }
}
