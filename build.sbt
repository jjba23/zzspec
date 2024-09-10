name := "zzspec"

ThisBuild / resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

addCommandAlias(
  "validate",
  ";compile;Test/compile;scalafmtSbt;scalafmtAll;scalafixAll"
)

val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides,
    addCompilerPlugin(KindProjector cross CrossVersion.full)
  )

enablePlugins(ZdRelease)
