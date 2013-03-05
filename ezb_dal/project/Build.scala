import sbt._
import Keys._

object EzbDalBuild extends Build {
       val name = "ezb-dal"
       val version = "1.0-SNAPSHOT"
       val buildScalaVersion = "2.10.0"

  resolvers ++= Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
  )

  lazy val root = Project(name, 
			  file("."),
			  settings = Defaults.defaultSettings ++ Seq (
			    scalaVersion := buildScalaVersion,
			    libraryDependencies ++= Seq(
			      "com.typesafe" % "slick_2.10.0-RC1" % "0.11.2",
			      "org.mindrot" % "jbcrypt" % "0.3m"
			    )
			  )
   )
}
