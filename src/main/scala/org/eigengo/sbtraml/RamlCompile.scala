package org.eigengo.sbtraml

import org.raml.parser.visitor.RamlDocumentBuilder
import sbt._
import sbt.Keys._

class RamlCompile(resourceLocation: File, s: TaskStreams) extends RamlSources {
  private val resourceLocationPath: String = resourceLocation.getAbsolutePath


  def compile(ramlFile: File): Unit = {
    val builder = new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath)

    // TODO: add !include validation
  }

  def run(): Unit = {
    val ramlFiles = findFiles(resourceLocation)
    s.log.info(s"Compiling ${ramlFiles.length} RAML files")

    ramlFiles.foreach(compile)
  }

}
