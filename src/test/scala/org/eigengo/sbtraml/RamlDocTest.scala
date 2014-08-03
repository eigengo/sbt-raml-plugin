package org.eigengo.sbtraml

import java.io.FileOutputStream

import org.scalatest.FlatSpec
import sbt.Keys._
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

  private def writeToFile(f: String)(content: String): Unit = {
    val fos = new FileOutputStream(f)
    fos.write(content.getBytes)
    fos.close()
  }

  "Simple RAML eyeball test" should "produce HTML documentation" in {
    val f = new File(getClass.getResource("/simple/").toURI)
    new RamlDoc(f, "classpath:///html.hbs", writeToFile("/Users/janmachacek/Desktop/x.html"), s()).run()
  }

}
