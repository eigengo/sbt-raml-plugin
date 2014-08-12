package org.eigengo.sbtraml

import java.io.FilenameFilter

import scala.io.Source

trait RamlSources {
  import java.io.File
  
  def findFiles(f: File): Array[File] = {
    val these = f.listFiles()
    these.filter(_.getName.endsWith(".raml")) ++ these.filter(_.isDirectory).flatMap(findFiles).filter(_.getName.endsWith(".raml"))
  }

  def load(f: File): String = Source.fromFile(f).mkString

}
