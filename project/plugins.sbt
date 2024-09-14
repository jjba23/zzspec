addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"      % "0.11.1")
addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.9.9" cross CrossVersion.full)
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"      % "2.5.2")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
