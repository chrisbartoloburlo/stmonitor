// Kludge to avoid building an empty .jar for the root project
Keys.`package` := {
  (Keys.`package` in (lchannels, Compile)).value
  (Keys.`package` in (monitor, Compile)).value
  (Keys.`package` in (examples, Compile)).value
  (Keys.`package` in (benchmarks, Compile)).value
}

lazy val commonSettings = Seq(
  version := "0.0.3",
  scalaVersion := "2.12.7",
  scalacOptions ++= Seq(
    "-target:jvm-1.8", "-unchecked", "-feature", "-Ywarn-unused-import" // "-deprecation"
  ),
  // ScalaDoc setup
  autoAPIMappings := true,
  scalacOptions in (Compile,doc) ++= Seq(
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
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    )
  )

lazy val examples = (project in file("examples")).
  dependsOn(lchannels, monitor).
  settings(commonSettings: _*).
  settings(
    name := "examples",

    libraryDependencies ++= Seq(
    )
  )

lazy val benchmarks = (project in file("benchmarks")).
  dependsOn(lchannels, monitor).
  settings(commonSettings: _*).
  settings(
    name := "benchmarks",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.15",
      "com.github.tototoshi" %% "scala-csv" % "1.3.6"
    )
    // Depending on the benchmark size and duration, you might want
    // to add the following options:
    //
    // fork := true, // Fork a JVM, running inside benchmarks/ dir
    // javaOptions ++= Seq("-Xms1024m", "-Xmx1024m") // Enlarge heap size
  )