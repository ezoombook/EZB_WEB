Ezb DAL Play Module
=====================================

This module allows to use the ezb_dal, which is play-independent, from a play application.
How it works :

1. Install the ezb_dal library to your local sbt repository:

    $cd <where ezb_dal lives>/ezb_dal
    $sbt clean publish-local

2. Install ezb_dal_play to your local sbt repository:

    $cd <where ezb_dal_play lives>/ezb_dal_play
    $sbt clean publish-local

3. Add ezb_dal_play to your dependencies:

  val appDependencies = Seq(
    "ezb-dal-play" % "ezb-dal-play_2.10" % "1.0-SNAPSHOT"
  )

Check the sample application to see it in action: [Application.scala](samples/dal_play_example/app/controllers/Application.scala)

Enjoy!

