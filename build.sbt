import scala.util.control.NonFatal
import xerial.sbt.Sonatype.sonatypeCentralHost
import xerial.sbt.Sonatype._

sonatypeProjectHosting := Some(
  GitHubHosting("jjba23", "zzspec", "jjbigorra@gmail.com")
)

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / publishTo := sonatypePublishToBundle.value

ThisBuild / version := "0.8.6"

name := "zzspec"

ThisBuild / resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

ThisBuild / versionScheme := Some("semver-spec")

val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides
  )
