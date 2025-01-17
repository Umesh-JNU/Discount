name := "Discount"

version := "1.4.0"

//Change to compile for a different scala version, e.g. 2.11.12
scalaVersion := "2.12.13"

//scapegoatVersion in ThisBuild := "1.3.9"
resolvers += "Spark Packages Repo" at "https://dl.bintray.com/spark-packages/maven"

libraryDependencies += "org.rogach" %% "scallop" % "latest.integration"

libraryDependencies += "org.scalatest" %% "scalatest" % "latest.integration" % "test"

libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "latest.integration" % "test"

//The "provided" configuration prevents sbt-assembly from including spark in the packaged jar.
//Change the version to compile for a different Spark version, e.g. 2.4.6
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.1.0" % "provided"

//Do not run tests during the assembly task
//(Running tests manually is still recommended)
test in assembly := {}

//Do not include scala library JARs in assembly (provided by Spark)
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "1")
