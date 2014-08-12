package org.eigengo.sbtraml

import org.scalatest.FeatureSpec
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

  feature("Manipulate file names") {
    val sourceDir = new File(getClass.getResource("/").toURI)
    val source    = new File(getClass.getResource("/subdirs/a/test.raml").toURI)

    scenario("Return proper target") {
      val target = new RamlDoc.RamlSource(source, sourceDir).target("html")

      assert(target === new File("subdirs/a/test.html"))
    }

    scenario("Reject bad extensions") {
      intercept[AssertionError] { new RamlDoc.RamlSource(source, sourceDir).target(".html") }
      intercept[AssertionError] { new RamlDoc.RamlSource(source, sourceDir).target("") }
    }

  }

  feature("Generate documentation from RAML files") {

    scenario("From single RAML file") {
      val f = new File(getClass.getResource("/simple/").toURI)
      new RamlDoc(f, "classpath:///html.hbs", (_, x) => println(x), s()).run()
    }

    scenario("From multiple RAML files in sub-directories") {
      val f = new File(getClass.getResource("/subdirs/").toURI)
      var counter = 0

      new RamlDoc(f, "classpath:///html.hbs", { (src, _) => println(src.target("html")); counter = counter + 1 }, s()).run()
      assert(counter === 2)
    }

  }

}
