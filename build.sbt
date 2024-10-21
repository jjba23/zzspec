import scala.util.control.NonFatal
import xerial.sbt.Sonatype.sonatypeCentralHost
import xerial.sbt.Sonatype._

ThisBuild / organization := "io.github.jjba23"
name := "zzspec"

ThisBuild / sonatypeProjectHosting := Some(
  GitHubHosting("jjba23", "zzspec", "jjbigorra@gmail.com")
)

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / publishTo := sonatypePublishToBundle.value

ThisBuild / version := "0.9.4"

ThisBuild / resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

ThisBuild / versionScheme := Some("semver-spec")

val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides
  )

ThisBuild / licenses := Seq(
  "LGPL3" -> url("https://www.gnu.org/licenses/lgpl-3.0.en.html")
)

ThisBuild / sonatypeProfileName := "io.github.jjba23"

ThisBuild / homepage := Some(url("https://github.com/jjba23/zzspec"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/jjba23/zzspec"),
    "scm:git@github.com:jjba23/zzspec.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "jjba23",
    name = "Josep Bigorra",
    email = "jjbigorra@gmail.com",
    url = url("https://github.com/jjba23")
  )
)

usePgpKeyHex("C19D36C1B0EFAACEA3E5EF1094C62486A9D59BEF")
