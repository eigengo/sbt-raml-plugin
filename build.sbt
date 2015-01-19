sbtPlugin := true

name := "sbt-raml"

organization := "org.eigengo"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.raml"                % "raml-parser"   % "0.8.10",
  "com.github.jknack"       % "handlebars"    % "2.0.0",
  "io.spray"               %% "spray-can"     % "1.3.2",
  "io.spray"               %% "spray-routing" % "1.3.2",
  "com.typesafe.akka"      %% "akka-actor"    % "2.3.8",
  "org.scalacheck"         %% "scalacheck"    % "1.12.1" % "test",
  "org.scalatest"          %% "scalatest"     % "2.2.3"  % "test"
)

resolvers += "Mulesoft" at "http://repository.mulesoft.org/releases"
