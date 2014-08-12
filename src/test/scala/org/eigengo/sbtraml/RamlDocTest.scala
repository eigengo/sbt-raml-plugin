package org.eigengo.sbtraml

import java.io.FileOutputStream

import org.scalatest.{FeatureSpec, FlatSpec}
import sbt.Keys._
import sbt._
import sbt.std.Streams

class RamlDocTest extends FeatureSpec {

  private def s(): Keys.TaskStreams = {
    val logger = new Logger {
      override def trace(t: => Throwable): Unit = ()
      override def log(level: Level.Value, message: => String): Unit = ()
      override def success(message: => String): Unit = ()
    }
    Streams[Def.ScopedKey[_]](_ => new File("."), _ => "", (_, _) => logger)(Def.ScopedKey[String](Scope.ThisScope, AttributeKey("x")))
  }

  private def writeToFile(f: RamlDoc.RamlSource, content: String): Unit = {
    val fos = new FileOutputStream(f.target("html"))
    fos.write(content.getBytes)
    fos.close()
  }

  feature("Generate documentation from RAML files") {

    scenario("From single RAML file") {
      val f = new File(getClass.getResource("/simple/").toURI)
      new RamlDoc(f, "classpath:///html.hbs", writeToFile, s()).run()
    }

    scenario("From multiple RAML files in sub-directories") {
      val f = new File(getClass.getResource("/subdirs/").toURI)
      var counter = 0

      new RamlDoc(f, "classpath:///html.hbs", (_, _) => counter = counter + 1, s()).run()
      assert(counter === 2)
    }

  }

}
