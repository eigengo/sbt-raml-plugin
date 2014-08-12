package org.eigengo.sbtraml

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.{StringTemplateSource, TemplateSource}
import org.raml.parser.visitor.RamlDocumentBuilder
import sbt.Keys._
import sbt._

import scala.io.Source

object RamlDoc {

  /**
   * Tha RAML soruce that is useful for generating the target file name, particularly for document generation
   *
   * @param source the source file
   * @param sourceDir the source directory
   */
  class RamlSource(source: File, sourceDir: File) {
    /**
     * Generates the target ``File`` for the given ``ext``. The returned ``File`` will include the relative
     * path from the source.
     *
     * For example, if ``source`` is ``/tmp/sbt-raml-plugin/foo/src/main/raml/a/b/X.raml`` and
     *              ``sourceDir`` is ``/tmp/sbt-raml-plugin/foo/src/main/raml``,
     * then the result of ``target("html")`` will be ``File`` with path ``a/b/X.html``. Notice the
     * relative path and the sub-directories that matches the source path.
     *
     * @param ext the required extension without the leading "."
     * @return ``File`` that matches the source
     */
    def target(ext: String): File = {
      assert(ext.length > 0)
      assert(ext.charAt(0) != '.')

      val pathOnly = source.getAbsolutePath.substring(sourceDir.getAbsolutePath.length + 1)
      new File(pathOnly.replaceAll("\\.[^.]*$", "") + "." + ext)
    }
  }

  /**
   * The file content
   */
  type Content = String
}

class RamlDoc(resourceLocation: File, template: String, write: (RamlDoc.RamlSource, RamlDoc.Content) => Unit, s: TaskStreams) extends RamlSources {
  private val handlebars = new Handlebars()
  handlebars.setInfiniteLoops(true)
  handlebars.setDeletePartialAfterMerge(true)
  handlebars.registerHelper("md", StringHelpers.lower)
  handlebars.registerHelper("lock", StringHelpers.lower)
  handlebars.registerHelper("eachValue", new EachValueHelper)

  private val resourceLocationPath: String = resourceLocation.getAbsolutePath

  private def findTemplateSource(template: String): TemplateSource = {
    if (template.startsWith("classpath://")) {
      val resource = getClass.getResource(template.substring(12))
      new StringTemplateSource(template, Source.fromURL(resource).mkString)
    } else {
      new StringTemplateSource(template, Source.fromFile(template).mkString)
    }
  }

  private def process(ramlFile: File): Unit = {
    try {
      val builder = new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath)
      val ct = handlebars.compile(findTemplateSource(template))
      val x = ct.apply(builder)
      write(new RamlDoc.RamlSource(ramlFile, resourceLocation), x)
    } catch {
      case x: Throwable =>
        s.log.error(s"${ramlFile}: ${x.getMessage}")
        throw x
    }
  }

  def run(): Unit = {
    val ramlFiles = findFiles(resourceLocation)
    s.log.info(s"Compiling ${ramlFiles.length} RAML files")

    ramlFiles.foreach(process)
  }

}
