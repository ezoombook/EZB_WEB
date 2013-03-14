Type `sbt doc` to generate the JavaDoc into `target/scala-2.1.0/api` 
Then add to your Build.scala:

    val appDependencies = Seq(
      // Add your project dependencies here,
      jdbc,
      "ezb-dal" % "ezb-dal_2.10" % "0.1-SNAPSHOT"
    )



