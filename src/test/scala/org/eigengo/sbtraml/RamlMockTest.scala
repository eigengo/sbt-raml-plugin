package org.eigengo.sbtraml

import org.scalatest.{GivenWhenThen, FeatureSpec}

class RamlMockTest extends FeatureSpec with GivenWhenThen {

  feature("RAML mock server") {
    Given("A well-formed RAML file")
    When("Client makes a request")
    Then("A response defined in the RAML file arrives")
  }

}
