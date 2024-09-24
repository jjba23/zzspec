import scala.util.control.NonFatal
import com.github.sbt.git.SbtGit.GitKeys

ThisBuild / version := "0.8.1"

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

val zzspec = project
  .settings(
    libraryDependencies ++= zzspecDependencies,
    dependencyOverrides ++= zzspecDependencyOverrides,
  )

githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

// enablePlugins(GhpagesPlugin)
// enablePlugins(SiteScaladocPlugin)

// git.remoteRepo := "git@personal.github.com:jjba23/zzspec.git"
// git.remoteRepo := "git@github.com:jjba23/zzspec.git"

// def pushSiteTask =
//   Def.task {
//     val git  = GitKeys.gitRunner.value
//     val repo = ghpagesSynchLocal.value
//     val s    = streams.value.log
//     git("add", ".")(repo, s)
//     try {
//       git("id", "personal")(repo, s)
//       val commit = "commit" +: ghpagesCommitOptions.value
//       git(commit: _*)(repo, s)
//     } catch {
//       case NonFatal(e) =>
//         s.info(e.toString)
//     }
//     git.push(repo, s)
//   }

// ghpagesPushSite := pushSiteTask.value
// val ZZSpec = config("zzspec")

// lazy val siteWithScaladoc = project
//   .in(file("site/scaladoc"))
//   .settings(
//     SiteScaladocPlugin.scaladocSettings(ZZSpec, zzspec / Compile / packageDoc / mappings, "api/zzspec")
//   )
