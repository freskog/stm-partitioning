name := "stm-partitioning"

version := "0.0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.0-RC10-1",
  "dev.zio" %% "zio-streams" % "1.0.0-RC10-1",
  "dev.zio" %% "zio-testkit" % "1.0.0-RC10-1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)