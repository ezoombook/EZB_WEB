import sbt._
import Keys._
import play.Project._
import java.io.File

object ApplicationBuild extends Build {

  val appName         = "ezb-dal-play"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "ezb-dal" %% "ezb-dal" % "0.1-SNAPSHOT" excludeAll(ExclusionRule(organization="play")), 
    jdbc,
    "com.typesafe.slick" %% "slick" % "1.0.0",
    "couchbase" % "couchbase-client" % "1.1.2"
  )

  val appResolvers = Seq(
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here   
//      scalaVersion := "2.10.0",
      resolvers ++= appResolvers
  ) 
}
