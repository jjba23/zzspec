clean:
	rm -rf .bloop .metals project/target project/project target
validate:
	sbt "scalafixAll; scalafmt; test"
test:
	sbt "test"
