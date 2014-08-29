package org.eigengo.sbtraml

import java.io.File

import akka.actor.{Props, ActorSystem, Actor}
import org.raml.model.Raml
import org.raml.parser.visitor.RamlDocumentBuilder
import spray.can.Http
import spray.http.{StatusCodes, HttpResponse, HttpRequest}

class RamlMock(resourceLocation: File, settings: MockSettings) extends RamlSources {
  private val resourceLocationPath: String = resourceLocation.getAbsolutePath
  private val ramlDefinitions = findFiles(resourceLocation).map(ramlFile => new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath))

  def start(): Unit = {
    val system = ActorSystem()
    val mockServer = system.actorOf(Props[MockServer])
    mockServer ! ramlDefinitions.toSeq
  }

}

class MockServer extends Actor {
  private var definitions: Seq[Raml] = Nil

  override def receive: Receive = {
    case Http.Connected(_, _) => sender ! Http.Register(self)
    case HttpRequest(method, uri, headers, entity, protocol) =>
      println("uri     -> " + uri)
      println("method  -> " + method)
      println("headers -> " + headers)
      println("entity  -> " + entity)

      sender ! HttpResponse(StatusCodes.NotImplemented)
    case newDefinitions: Seq[Raml] => definitions = newDefinitions
  }
}