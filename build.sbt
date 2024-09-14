ThisBuild / name := "zzspec"

ThisBuild / resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides,
  )

ThisBuild / organizationHomepage := Some(url("https://github.com/jjba23/zzspec"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/jjba23/zzspec"),
    "scm:git@github.com:jjba23/zzspec.git"
  )
)

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

inThisBuild(
  List(
    organization := "zzspec",
    homepage := Some(url("https://github.com/jjba23/zzspec")),
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
