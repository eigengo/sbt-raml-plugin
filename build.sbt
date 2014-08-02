sbtPlugin := true

name := "sbt-raml"

organization := "org.eigengo"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.raml"        % "raml-parser" % "0.8.7",
  "org.scalacheck" %% "scalacheck"  % "1.11.5"
)

resolvers += "Mulesoft" at "http://repository.mulesoft.org/releases"
