name := "zzspec"

ThisBuild / resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides,
  )
