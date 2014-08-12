package org.eigengo.sbtraml

import scala.io.Source

/**
 * Contains convenience functions for manipulating (RAML) files
 */
trait RamlSources {
  import java.io.File

  /**
   * Finds all ``*.raml`` files in the directory ``f``
   *
   * @param f the directory to start finding the files in
   * @return the Array of ``*.raml`` files in all sub-directories of ``f``
   */
  def findFiles(f: File): Array[File] = {
    def isRaml(f: File): Boolean = f.getName.endsWith(".raml")

    val these = f.listFiles()
    these.filter(isRaml) ++ these.filter(_.isDirectory).flatMap(findFiles).filter(isRaml)
  }

  /**
   * Loads the contents of ``f`` as ``String``
   * @param f the file to load
   * @return the contents of the file
   */
  def load(f: File): String = Source.fromFile(f).mkString

}
