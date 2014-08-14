package org.eigengo.sbtraml

import org.raml.parser.visitor.RamlDocumentBuilder
import sbt.Keys._
import sbt._

/**
 * Verifies all RAML files in all sub-directories of ``resourceLocation``
 *
 * @param resourceLocation the "root" directory containing all the RAML files
 * @param s the TaskStreams to report progress & errors
 */
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
