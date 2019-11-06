name := "stm-partitioning"

version := "0.0.1"
scalaVersion := "2.12.10"
scalacOptions += ("-deprecation")
val zioVersion       = "1.0.0-RC16"
val scalaTestVersion = "3.0.8"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio"          % zioVersion,
  "dev.zio" %% "zio-test"     % zioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
)
testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

addCommandAlias("com", "all compile test:compile")
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
