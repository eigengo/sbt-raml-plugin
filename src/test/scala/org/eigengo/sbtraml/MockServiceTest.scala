package org.eigengo.sbtraml

import java.io.File

import akka.actor.ActorSystem
import org.raml.parser.visitor.RamlDocumentBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import spray.http.{HttpMethods, HttpRequest, StatusCodes, Uri}

class MockServiceTest extends FeatureSpec with Matchers with GivenWhenThen with ScalaFutures with MockService with RamlSources {
  private val resourceLocation = new File(getClass.getResource("/").toURI)
  private val resourceLocationPath: String = resourceLocation.getAbsolutePath
  private val ramlDefinitions = findFiles(resourceLocation).map(ramlFile => new RamlDocumentBuilder().build(load(ramlFile), resourceLocationPath)).toList

  feature("Service with type references") {
    implicit val system = ActorSystem()
    import system.dispatcher

    Given("Well-formed RAML files for the service")

    When("Clent makes requests")
    val get = mock(ramlDefinitions, MockSettings(), HttpRequest(HttpMethods.GET, Uri("/configuration/some/other/foo")))

    Then("A response defined in the RAML file arrives")
    whenReady(get)(println)

    system.shutdown()
  }

  feature("Service without types") {
    implicit val system = ActorSystem()
    import system.dispatcher

    Given("Well-formed RAML files for the services")

    When("Client makes known requests")
    val get = mock(ramlDefinitions, MockSettings(), HttpRequest(HttpMethods.GET, Uri("/1.2.3/streams/my-stream")))
    val getF = mock(ramlDefinitions, MockSettings(), HttpRequest(HttpMethods.GET, Uri("/1.2.3/streams/my-stream-more-than-10")))
    val put = mock(ramlDefinitions, MockSettings(), HttpRequest(HttpMethods.PUT, Uri("/1.2.3/streams/my-stream")))
    val putF = mock(ramlDefinitions, MockSettings(), HttpRequest(HttpMethods.PUT, Uri("/1.2.3/streams/my-stream-more-than-10")))

    Then("A response defined in the RAML file arrives")
    whenReady(put)(println)
    whenReady(getF)(res => res.status should be(StatusCodes.NotFound))
    whenReady(putF)(res => res.status should be(StatusCodes.NotFound))
    whenReady(get) { res =>
      println(res)
      res.status should be(StatusCodes.OK)
    }

    system.shutdown()
  }

}
