clean:
	rm -rf .bloop .metals project/target project/project target zzspec/target
	GITHUB_TOKEN="" sbt "bloopInstall"
validate:
	GITHUB_TOKEN=""	sbt "scalafixAll; scalafmt; test"
fmt:
	GITHUB_TOKEN=""	sbt "scalafixAll; scalafmt"
test:
	GITHUB_TOKEN=""	sbt "test"
release:
	sbt "publishSigned"
	sbt "sonatypeBundleRelease"
