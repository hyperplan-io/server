val Http4sVersion = "0.20.0-M5"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val ScalazZIOVersion = "1.0-RC1"
val circeVersion = "0.11.1"
val DoobieVersion = "0.7.0-M3"

lazy val root = (project in file("."))
  .settings(
    organization := "foundaml",
    name := "foundaml-server",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings"
    ),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalaz" %% "scalaz-zio" % ScalazZIOVersion,
      "org.scalaz" %% "scalaz-zio-interop-cats" % ScalazZIOVersion,
      "org.scalaz" %% "scalaz-zio-interop-future" % ScalazZIOVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.amazonaws" % "aws-java-sdk-kinesis" % "1.11.514",
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-h2" % DoobieVersion % "test",
      "org.tpolecat" %% "doobie-scalatest" % DoobieVersion % "test",
      "com.github.pureconfig" %% "pureconfig" % "0.10.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "com.google.cloud" % "google-cloud-pubsub" % "1.66.0"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),
    micrositeName := "FoundaML",
    micrositeDescription := "Pipeline for machine learning algorithms",
    micrositeAuthor := "FoundaML contributors",
    micrositeOrganizationHomepage := "https://github.com/antoinesauray/foundaml-server",
    micrositeGitterChannelUrl := "antoinesauray/foundaml-server",
    micrositeGithubOwner := "antoinesauray",
    micrositeGithubRepo := "foundaml-server",
    micrositeFavicons := Seq(
      microsites.MicrositeFavicon("favicon.png", "512x512")
    ),
    micrositeUrl := "https://antoinesauray.github.io",
    micrositeBaseUrl := "/foundaml-server"
  )
  .enablePlugins(MicrositesPlugin)
