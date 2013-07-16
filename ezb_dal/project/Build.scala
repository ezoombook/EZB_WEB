import sbt._
import Keys._

object EzbDalBuild extends Build {
       val name = "ezb-dal"
       val version = "1.0-SNAPSHOT"
       val buildScalaVersion = "2.10.0"

  val libresolvers = Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
      "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/",
      "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/",
      "Couchbase Maven Repository" at "http://files.couchbase.com/maven2",
      "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + "/.m2/repository"
  )

  val libdependencies = Seq(
      "com.typesafe.slick" %% "slick" % "1.0.0",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "play"        % "play-json_2.10" % "2.2-SNAPSHOT",
      "couchbase" % "couchbase-client" % "1.1.6" ,
      "nl.siegmann.epublib" %% "epublib-core" % "3.1"
  )

  lazy val root = Project(name, 
			  file("."),
			  settings = Defaults.defaultSettings ++ Seq (
			    scalaVersion := buildScalaVersion,
			    resolvers := libresolvers,
			    libraryDependencies ++= libdependencies
			  )
   )
}
