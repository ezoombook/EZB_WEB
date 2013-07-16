// Comment to get more information during initialization
logLevel := Level.Warn

//scalaVersion := "2.10.1"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.2") //,"0.12","2.9.2")

publishArtifact in (Compile, packageDoc) := false
