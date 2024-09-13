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

credentials += Credentials(
  "GnuPG Key ID",
  "gpg",
  "C19D36C1B0EFAACEA3E5EF1094C62486A9D59BEF", // key identifier
  "ignored" // this field is ignored; passwords are supplied by pinentry
)

organization := "zzspec"
organizationHomepage := Some(url("https://jointhefreeworld.org"))

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
    url = url("https://jointhefreeworld.org")
  )
)

ThisBuild / description := "A Scala library with great ZIO integration to help you easily write high-level integration/black box tests. ZZSpec tests help you grow your confidence in the correctness of software."
ThisBuild / licenses := List(
  "GNU GPL v3" -> new URL("https://www.gnu.org/licenses/gpl-3.0.en.html")
)
ThisBuild / homepage := Some(url("https://github.com/jjba23/zzspec"))

ThisBuild / versionScheme := Some("early-semver")

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("jjba23", "zzspec", "jjbigorra@gmail.com"))

// publish to the sonatype repository
publishTo := sonatypePublishToBundle.value
