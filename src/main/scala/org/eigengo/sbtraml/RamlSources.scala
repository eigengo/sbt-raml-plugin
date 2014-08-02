package org.eigengo.sbtraml

import java.io.FilenameFilter

import scala.io.Source

trait RamlSources {
  import java.io.File
  
  def findFiles(f: File): Array[File] = {
    val these = f.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".raml")
    })
    these ++ these.filter(_.isDirectory).flatMap(findFiles)
  }

  def load(f: File): String = Source.fromFile(f).mkString

}
