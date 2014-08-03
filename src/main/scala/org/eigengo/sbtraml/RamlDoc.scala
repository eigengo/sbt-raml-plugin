package org.eigengo.sbtraml

import java.io.FileOutputStream

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.{StringTemplateSource, TemplateSource}
import org.raml.parser.visitor.RamlDocumentBuilder
import sbt.Keys._
import sbt._

import scala.io.Source

class RamlDoc(resourceLocation: File, template: String, s: TaskStreams) extends RamlSources {
  private val handlebars = new Handlebars()
  handlebars.setInfiniteLoops(true)
  handlebars.setDeletePartialAfterMerge(true)
  handlebars.registerHelper("md", StringHelpers.lower)
  handlebars.registerHelper("lock", StringHelpers.lower)
  handlebars.registerHelper("eachValue", new EachValueHelper)

  private val resourceLocationPath: String = resourceLocation.getAbsolutePath

  def findTemplateSource(template: String): TemplateSource = {
    if (template.startsWith("classpath://")) {
      val resource = getClass.getResource(template.substring(12))
      new StringTemplateSource(template, Source.fromURL(resource).mkString)
    } else {
      new StringTemplateSource(template, Source.fromFile(template).mkString)
    }
  }

  def process(ramlFile: File): Unit = {
    val builder = new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath)

    val ct = handlebars.compile(findTemplateSource(template))
    val x = ct.apply(builder)
    val fos = new FileOutputStream("/Users/janmachacek/Desktop/x.html")
    fos.write(x.getBytes)
    fos.close
  }

  def run(): Unit = {
    val ramlFiles = findFiles(resourceLocation)
    s.log.info(s"Compiling ${ramlFiles.length} RAML files")

    ramlFiles.foreach(process)
  }

}
