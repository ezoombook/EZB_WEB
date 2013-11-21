import sbt._
import Keys._
import play.Project._
import java.io.File

object ApplicationBuild extends Build {

  val appName         = "ezb-dal-play"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "ezb-dal" %% "ezb-dal" % "0.1-SNAPSHOT" excludeAll(ExclusionRule(organization="play"),ExclusionRule(organization="nl")),
    jdbc,
    "com.typesafe.slick" %% "slick" % "1.0.0",
    "com.couchbase.client" % "couchbase-client" % "1.2.2"
  )

  val appResolvers = Seq(
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "eZoomBook repoisitory" at "https://github.com/ezoombook/ezb-mvn/raw/master"
  )

  val localMavenRepo = Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/Developement/ezoombook/mvn-repo")))

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here   
      scalaVersion := "2.10.0",
      resolvers ++= appResolvers,
      publishTo := localMavenRepo,
      publishArtifact in (Compile, packageSrc) := false,
      publishArtifact in (Compile, packageDoc) := false
  ) 
}
