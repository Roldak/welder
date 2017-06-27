
name := "Welder"
version := "0.1"
organization := "ch.lara.epfl"

scalaVersion := "2.11.8"
scalacOptions += "-feature"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "example" / "scala"