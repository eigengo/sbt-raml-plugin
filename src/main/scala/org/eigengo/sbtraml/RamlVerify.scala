package org.eigengo.sbtraml

import org.raml.parser.visitor.RamlDocumentBuilder
import sbt.Keys._
import sbt._

class RamlVerify(resourceLocation: File, s: TaskStreams) extends RamlSources {
  private val resourceLocationPath: String = resourceLocation.getAbsolutePath


  def verify(ramlFile: File): Unit = {
    val builder = new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath)
    // TODO: add !include validation
  }

  def run(): Unit = {
    val ramlFiles = findFiles(resourceLocation)
    s.log.info(s"Compiling ${ramlFiles.length} RAML files")

    ramlFiles.foreach(verify)
  }

}
