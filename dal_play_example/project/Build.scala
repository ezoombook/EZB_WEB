import sbt._
import Keys._
import play.Project._
import java.io.File

object ApplicationBuild extends Build {

  val appName         = "example"
  val appVersion      = "1.0-SNAPSHOT"
  val localIvyPath    = Path.userHome + File.separator + ".ivy2" + File.separator + "local"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "ezb-dal-play" % "ezb-dal-play_2.10" % "1.0-SNAPSHOT"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here    
    templatesImport += "users.dal._"
  )

}
