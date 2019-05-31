name := "stm-partitioning"

version := "0.0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-zio" % "1.0-RC4",
  "org.scalaz" %% "scalaz-zio-streams" % "1.0-RC4",
  "org.scalaz" %% "scalaz-zio-testkit" % "1.0-RC4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)