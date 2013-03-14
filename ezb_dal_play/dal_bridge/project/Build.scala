import sbt._
import Keys._
import play.Project._
import java.io.File

object ApplicationBuild extends Build {

  val appName         = "ezb-dal-play"
  val appVersion      = "1.0-SNAPSHOT"
  val localIvyPath    = Path.userHome + File.separator + ".ivy2" + File.separator + "local"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "ezb-dal" % "ezb-dal_2.10" % "0.1-SNAPSHOT"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here    
  )

}
