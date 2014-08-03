package org.eigengo.sbtraml

import org.scalatest.FlatSpec
import sbt._
import sbt.std.Streams

class RamlDocTest extends FlatSpec {

  private def s(): Keys.TaskStreams = {
    val logger = new Logger {
      override def trace(t: => Throwable): Unit = ()
      override def log(level: Level.Value, message: => String): Unit = ()
      override def success(message: => String): Unit = ()
    }
    Streams[Def.ScopedKey[_]](_ => new File("."), _ => "", (_, _) => logger)(Def.ScopedKey[String](Scope.ThisScope, AttributeKey("x")))
  }

  "Simple RAML eyeball test" should "produce HTML documentation" in {
    val f = new File(getClass.getResource("/simple/").toURI)
    new RamlDoc(f, "classpath:///html.hbs", println, s()).run()
  }

}
