package org.eigengo.sbtraml

import sbt.Keys._
import sbt._

trait RamlSettings {
  import autoImport._

  lazy val settings = (compile in Compile) <<= (compile in Compile).dependsOn(ramlCompile in Compile)

  object autoImport {

    val ramlCompile = taskKey[Unit]("Process RAML files")
    val ramlDoc     = taskKey[Unit]("Process RAML files")
    val ramlSource  = settingKey[File]("RAML source directories")

    lazy val baseRamlSettings: Seq[Setting[_]] = Seq(
      ramlCompile := { RamlCompile((ramlSource in Compile).value) },
      ramlDoc := { RamlDoc((ramlSource in Compile).value) },
      ramlSource in Compile := file("src/raml")
    )
  }


}

object RamlPlugin extends AutoPlugin with RamlSettings {

  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(baseRamlSettings)
}
