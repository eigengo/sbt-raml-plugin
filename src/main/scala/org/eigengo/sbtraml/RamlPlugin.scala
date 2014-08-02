package org.eigengo.sbtraml

import sbt.Keys._
import sbt._

trait RamlSettings {
  import autoImport._

  private lazy val compileSettings = (compile in Compile) <<= (compile in Compile).dependsOn(compile in Raml)
  private lazy val docSettings = publishLocal <<= publishLocal dependsOn (doc in Raml) // <<= (ramlDoc in Raml) dependsOn publishLocal

  lazy val settings = docSettings ++ compileSettings

  object autoImport {

    val Raml    = config("raml")
    val compile = taskKey[Unit]("Compile RAML sources")
    val doc     = taskKey[Unit]("Generate human-friendly documentation from the RAML sources")
    val source  = settingKey[File]("RAML source directory")

    lazy val baseRamlSettings: Seq[Setting[_]] = Seq(
      compile := { new RamlCompile((source in Raml).value, streams.value).run() },
      doc := { new RamlDoc((source in Raml).value, streams.value).run() },
      source in Raml := baseDirectory.value / "src/raml"
    )
  }

}

object RamlPlugin extends AutoPlugin with RamlSettings {

  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Raml)(baseRamlSettings)
}
