// Kludge to avoid building an empty .jar for the root project
Keys.`package` := {
  (lchannels / Compile / Keys.`package`).value
  (monitor / Compile / Keys.`package`).value
  (examples / Compile / Keys.`package`).value
}

lazy val commonSettings = Seq(
  version := "0.0.3",
  scalaVersion := "2.12.13",
  scalacOptions ++= Seq(
    "-unchecked", "-feature", "-Ywarn-unused-import" // "-deprecation"
  ),
  // ScalaDoc setup
  autoAPIMappings := true,
  Compile / doc / scalacOptions ++= Seq(
    "-no-link-warnings" // Workaround for ScalaDoc @throws links issues
  )
)

lazy val lchannels = (project in file("lchannels")).
  settings(commonSettings: _*).
  settings(
    name := "lchannels",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-typed" % "2.5.0",
      "com.typesafe.akka" %% "akka-remote" % "2.5.0"
    )
  )

lazy val monitor = (project in file("monitor")).
  settings(commonSettings: _*).
  settings(
    name := "monitor",

    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
      "com.github.tototoshi" %% "scala-csv" % "1.3.6"
    )
  )

lazy val examples = (project in file("examples")).
  dependsOn(lchannels, monitor).
  settings(commonSettings: _*).
  settings(
    name := "examples",

    generateMonitors := (Def.taskDyn {
      val baseDir = sourceDirectory.value / "main" / "scala" / "examples"
      Def.task {
        generateMonitor(baseDir, "coin").value
        generateMonitor(baseDir, "game").value
        generateMonitor(baseDir, "smtp").value
      }
    }).value,

    (Compile / compile) := ((Compile / compile) dependsOn generateMonitors).value,

    libraryDependencies ++= Seq(
      "com.athaydes.rawhttp" % "rawhttp-core" % "2.4.0",
    )
  )

val generateMonitors = taskKey[Unit]("Generate session monitors.")

def generateMonitor(baseDir: File, name: String) = {
  val exampleDir = baseDir / name
  val stFile = exampleDir / (name ++ ".st")
  val preamble = exampleDir / "preamble.txt"
  (monitor / Compile / runMain).toTask(f" monitor.Generate ${exampleDir} ${stFile} ${preamble} wilson")
}
