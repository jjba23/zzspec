clean:
	rm -rf .bloop .metals project/target project/project target zzspec/target
	sbt "bloopInstall"
validate:
	sbt "scalafixAll; scalafmt; test"
fmt:
	sbt "scalafixAll; scalafmt"
test:
	sbt "test"
release:
# this requires the correct GPG key to be present in your system	
	sbt "publishSigned"
	sbt "sonatypeBundleRelease"
