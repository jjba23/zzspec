ThisBuild / version := "0.7.10"

name := "zzspec"

ThisBuild / resolvers += "Mulesoft".at(
  "https://repository.mulesoft.org/nexus/content/repositories/public/"
)

ThisBuild / organizationHomepage := Some(url("https://github.com/jjba23/zzspec"))

ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / developers := List(Developer(id = "jjba23", name = "Josep Bigorra", email = "jjbigorra@gmail.com", url = url("https://github.com/jjba23")))

ThisBuild / githubOwner := "jjba23"
ThisBuild / githubRepository := "zzspec"

publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
Compile / pushRemoteCacheConfiguration := null

lazy val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides,
  )

enablePlugins(GhpagesPlugin)
enablePlugins(SiteScaladocPlugin)

git.remoteRepo := "git@github.com:jjba23/zzspec.git"
