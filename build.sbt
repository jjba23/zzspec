name := "zzspec"
ThisBuild / version := "0.7.2"

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

ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / developers := List(Developer(id = "jjba23", name = "Josep Bigorra", email = "jjbigorra@gmail.com", url = url("https://github.com/jjba23")))

ThisBuild / githubOwner := "jjba23"
ThisBuild / githubRepository := "zzspec"
