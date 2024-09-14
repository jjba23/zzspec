clean:
	rm -rf .bloop .metals project/target project/project target zzspec/target
	GITHUB_TOKEN="" sbt "bloopInstall"
validate:
	GITHUB_TOKEN=""	sbt "scalafixAll; scalafmt; test"
test:
	GITHUB_TOKEN=""	sbt "test"
