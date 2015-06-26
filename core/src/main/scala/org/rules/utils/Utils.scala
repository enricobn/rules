package org.rules.utils

import java.io.File

import org.rules.rule.Logged

/**
 * Created by enrico on 6/7/15.
 */
object Utils {

  def recursiveListFiles(folder: File, filter: File => Boolean): Logged[Array[File]] = {
    val these = folder.listFiles
    if (these == null) {
      return "Error getting files from " + folder
    }

    val files = these.filter(_.isDirectory).foldLeft(Logged(Some(Array.empty[File]))){ (actual,ff) =>
      actual.fold(recursiveListFiles(ff, filter))(_++_)
    }

    files.fold(these.filter{f => f.isFile && filter(f) })(_++_)
  }

  def delete(file: File) {
    if (file.isDirectory)
      Option(file.listFiles).map(_.toList).getOrElse(Nil).foreach(delete(_))
    file.delete
  }

}
