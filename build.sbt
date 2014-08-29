sbtPlugin := true

name := "sbt-raml"

organization := "org.eigengo"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.raml"                % "raml-parser"   % "0.8.7",
  "com.github.jknack"       % "handlebars"    % "1.3.1",
  "io.spray"                % "spray-can"     % "1.3.1",
  "com.typesafe.akka"      %% "akka-actor"    % "2.3.5",
  "org.scalacheck"         %% "scalacheck"    % "1.11.5" % "test",
  "org.scalatest"          %% "scalatest"     % "2.2.1"  % "test"
)

resolvers += "Mulesoft" at "http://repository.mulesoft.org/releases"
