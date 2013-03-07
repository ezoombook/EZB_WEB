import sbt._
import Keys._

object EZBuild extends Build {
  lazy val root = Porject(id="ezbook",
			  base = file(".")) aggregate (ezb-dal, ezb-play-example)

  lazy val ezb-dal = Project(id="ezb-dal",
			     base = file("ezb-dal"))

  lazy val ezb-play-example = Project(id="ezb-play-example",
				      base = file("ezb-play-example")) dependsOn(ezb-dal)
}
