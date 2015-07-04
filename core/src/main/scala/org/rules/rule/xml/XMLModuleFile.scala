package org.rules.rule.xml

import java.io.{File, FileInputStream}

import scala.xml.XML

/**
  * Created by enrico on 6/27/15.
  */
case class XMLModuleFile(/*id: String, */file: File) {
   val name = file.getName().substring(0,file.getName.length - 10)

   lazy val xmlModule = XMLModule(name, XML.load(new FileInputStream(file)))

   def updateAndSave(changedRules: Seq[XMLRule], deletedRulesIds: Seq[String]) : XMLModuleFile = {
     val updated = xmlModule.update(changedRules, deletedRulesIds)
     updated.save(file)

     new XMLModuleFile(/*id,*/ file) {
       // since I don't want to reload the file!
       override lazy val xmlModule = updated
     }
   }

   def delete() = file.delete()

 }
