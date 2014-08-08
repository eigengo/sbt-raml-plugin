publishMavenStyle := true

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/eigengo/sbt-raml-plugin</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:eigengo/sbt-raml-plugin.git</url>
    <connection>scm:git:git@github.com:eigengo/sbt-raml-plugin.git</connection>
  </scm>
  <developers>
    <developer>
      <id>janm399</id>
      <name>Jan Machacek</name>
      <url>http://www.eigengo.com</url>
    </developer>
  </developers>)
