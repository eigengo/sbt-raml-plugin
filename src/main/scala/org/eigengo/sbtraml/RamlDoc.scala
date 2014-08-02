package org.eigengo.sbtraml

import sbt.Keys._
import sbt._

class RamlDoc(resourceLocation: File, s: TaskStreams) extends RamlSources {

  def run(): Unit = {
    println("*************** " + resourceLocation)
  }

}
