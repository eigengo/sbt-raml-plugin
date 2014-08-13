package org.eigengo.sbtraml

import java.io.FileOutputStream

import org.apache.commons.io.FilenameUtils
import sbt.Keys._
import sbt._

/**
 * Defines settings for the RAML plugin.
 */
trait RamlSettings {

  private lazy val compileSettings = (compile in Compile) <<= (compile in Compile).dependsOn(Keys.verify in Keys.Raml)
  private lazy val docSettings = publishLocal <<= publishLocal dependsOn (Keys.html in Keys.Raml) // <<= (ramlDoc in Raml) dependsOn publishLocal

  lazy val settings = docSettings ++ compileSettings

  object Keys {

    val Raml     = config("raml")
    val verify   = taskKey[Unit]("Verify RAML sources")
    val html     = taskKey[Unit]("Generate human-friendly documentation from the RAML sources")
    val source   = settingKey[File]("RAML source directory")
    val template = settingKey[File]("Template source")

    private def writeToFile(s: TaskStreams, target: File)(f: RamlDoc.RamlSource, content: String): Unit = {
      val targetFile = f.target("html")
      val directoryAvailable = if (targetFile.getParent != null) new File(FilenameUtils.concat(target.getAbsolutePath, targetFile.getParent)).mkdirs() else true
      if (directoryAvailable) {
        val htmlFile = FilenameUtils.concat(target.getAbsolutePath, targetFile.getPath)
        val fos = new FileOutputStream(htmlFile)
        fos.write(content.getBytes)
        fos.close()

        s.log.info("Written RAML documentation to " + targetFile)
      } else {
        s.log.error("Could not create directory for " + targetFile)
      }
    }

    lazy val baseRamlSettings: Seq[Setting[_]] = Seq(
      verify := { new RamlVerify((source in Raml).value, streams.value).run() },
      html := { new RamlDoc((source in Raml).value, (template in Raml).?.value , writeToFile(streams.value, (target in Compile).value), streams.value).run() },
      source in Raml := baseDirectory.value / "src/raml"
    )
  }

}

/**
 * The entry-point for the RAML plugin
 */
object RamlPlugin extends AutoPlugin with RamlSettings {

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Keys.Raml)(Keys.baseRamlSettings)
}
