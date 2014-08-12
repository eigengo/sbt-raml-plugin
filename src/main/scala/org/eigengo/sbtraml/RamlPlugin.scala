package org.eigengo.sbtraml

import java.io.FileOutputStream

import sbt.Keys._
import sbt._

trait RamlSettings {

  private lazy val compileSettings = (compile in Compile) <<= (compile in Compile).dependsOn(Keys.verify in Keys.Raml)
  private lazy val docSettings = publishLocal <<= publishLocal dependsOn (Keys.html in Keys.Raml) // <<= (ramlDoc in Raml) dependsOn publishLocal

  lazy val settings = docSettings ++ compileSettings

  object Keys {

    val Raml    = config("raml")
    val verify  = taskKey[Unit]("Verify RAML sources")
    val html    = taskKey[Unit]("Generate human-friendly documentation from the RAML sources")
    val source  = settingKey[File]("RAML source directory")
    val template = settingKey[String]("Template source")

    private def writeToFile(s: TaskStreams)(f: RamlDoc.RamlSource, content: String): Unit = {
      val fos = new FileOutputStream(f.target("html"))
      fos.write(content.getBytes)
      fos.close()

      s.log.info("Written RAML documentation to " + f)
    }

    lazy val baseRamlSettings: Seq[Setting[_]] = Seq(
      verify := { new RamlVerify((source in Raml).value, streams.value).run() },
      html := { new RamlDoc((source in Raml).value, (template in Raml).value, writeToFile(streams.value), streams.value).run() },
      source in Raml := baseDirectory.value / "src/raml",
      template in Raml := "classpath:///html.hbs"
    )
  }

}

object RamlPlugin extends AutoPlugin with RamlSettings {

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Keys.Raml)(Keys.baseRamlSettings)
}
