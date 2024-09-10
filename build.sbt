name := "zzspec"

ThisBuild / resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides,
  )
