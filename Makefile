clean:
	rm -rf .bloop .metals project/target project/project target zzspec/target
validate:
	sbt "scalafixAll; scalafmt; test"
test:
	sbt "test"
