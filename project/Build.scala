import sbt._
import Keys._
import play.Project._
import java.io.File

object ApplicationBuild extends Build {

  val appName         = "ezb-web"
  val appVersion      = "1.0-SNAPSHOT"
  val localIvyPath    = Path.userHome + File.separator + ".ivy2" + File.separator + "local"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    //cache,
    //("ezb-dal-play" % "ezb-dal-play_2.10" % "2.0-SNAPSHOT")
    //  .exclude("org.scala-stm", "scala-stm_2.10.0")
    //  .exclude("com.typesafe.play", "*"),
    ("jp.t2v" %% "play2-auth"      % "0.11.0").exclude("com.typesafe.play", "*"),
    //("jp.t2v" %% "play2-auth-test" % "0.11.0" % "test").exclude("play", "*"),
    ("org.reactivecouchbase" %% "reactivecouchbase-play" % "0.2-SNAPSHOT").exclude("com.typesafe.play", "*"),
    "org.postgresql" % "postgresql" % "9.3-1100-jdbc41"
  )

  val appResolvers = Seq(
    "eZoomBook repoisitory" at "https://github.com/ezoombook/ezb-mvn/raw/master",
    "ReactiveCouchbase" at "https://raw.github.com/ReactiveCouchbase/repository/master/snapshots",
    "ReactiveCouchbase Releases" at "https://raw.github.com/ReactiveCouchbase/repository/master/releases/"
  )

  // Data application layer
  val ezb_dal_play = Project(id="ezb-dal-play", base=file("dal/ezb_dal_play/dal_bridge"))
  val ezb_dal = Project(id="ezb-dal", base=file("dal/ezb_dal"))

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here    
    templatesImport ++= Seq("users.dal._","books.dal._", "project.dal._", "ezb.comments._", "forms._") ,
    lessEntryPoints <<= baseDirectory(customLessEntryPoints),
    resolvers ++= appResolvers
  ) dependsOn(ezb_dal, ezb_dal_play)

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory
  def customLessEntryPoints(base: File): PathFinder =
    ( (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
      (base / "app" / "assets" / "stylesheets" * "*.less") )
}
