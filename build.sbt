import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.sonatypeCentralHost

name := "zzspec"

resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides,
  )

organization := "zzspec"
organizationHomepage := Some(url("https://github.com/jjba23/zzspec"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/jjba23/zzspec"),
    "scm:git@github.com:jjba23/zzspec.git"
  )
)

homepage := Some(url("https://github.com/jjba23/zzspec"))

versionScheme := Some("early-semver")

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

inThisBuild(
  List(
    organization := "com.github.sbt",
    homepage := Some(url("https://github.com/sbt/sbt-ci-release")),
    licenses := List(
      "GNU GPL v3" -> new URL("https://www.gnu.org/licenses/gpl-3.0.en.html")
    ),
    developers :=
      List(
        Developer(
          id = "jjba23",
          name = "Josep Bigorra",
          email = "jjbigorra@gmail.com",
          url = url("https://jointhefreeworld.org")
        )
      )
  )
)
