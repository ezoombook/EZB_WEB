import sbt._
import Keys._

object ezoombook extends Build {
  val name = "ezoombook"
  val buildScalaVersion = "2.10.0"

  lazy val ezoomproject = Project(name, file("."),
			  settings = Defaults.defaultSettings ++ Seq (
			    scalaVersion := buildScalaVersion
			  )) aggregate(ezb_dal, ezb_bridge, play_example, ezb_web)

  lazy val play_example = RootProject(file("dal_play_example"))
  lazy val ezb_bridge = RootProject(file("ezb_dal_play/dal_bridge")) 
  lazy val ezb_dal = RootProject(file("ezb_dal"))
  lazy val ezb_web = RootProject(file("ezb_web"))
}
