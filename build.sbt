val Http4sVersion = "0.20.0"
val Specs2Version = "4.1.0"
val circeVersion = "0.11.1"
val DoobieVersion = "0.6.0"

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
    fork in (IntegrationTest, run) := true
  ).aggregate(app, microsite)
    
  lazy val app = project.in(file("app"))
    .settings(
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
      "org.typelevel" %% "cats-core" % "1.6.0",
      "org.typelevel" %% "cats-effect" % "1.2.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.amazonaws" % "aws-java-sdk-kinesis" % "1.11.534" excludeAll(
        ExclusionRule(organization = "org.jboss.logging")
      ),
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-h2" % DoobieVersion % "test",
      "org.tpolecat" %% "doobie-scalatest" % DoobieVersion % "test",
      "com.github.pureconfig" %% "pureconfig" % "0.10.2",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.google.cloud" % "google-cloud-pubsub" % "1.66.0" excludeAll (
        ExclusionRule(organization = "com.fasterxml.jackson.core"),
        ExclusionRule(organization = "com.fasterxml.jackson.dataformat"),
        ExclusionRule(organization = "org.jboss.logging")
      ),
      "co.fs2" %% "fs2-core" % "1.0.4",
      "co.fs2" %% "fs2-io" % "1.0.4",
      "com.ovoenergy" %% "fs2-kafka" % "0.19.9",
      "com.kubukoz" %% "sup-core" % "0.4.0",
      "com.kubukoz" %% "sup-doobie" % "0.4.0",
      "io.kamon" %% "kamon-core" % "1.1.0",
      "io.kamon" %% "kamon-prometheus" % "1.1.1",
      "io.kamon" %% "kamon-jdbc" % "1.0.2",
      "io.kamon" %% "kamon-logback" % "1.0.6",
      "io.kamon" %% "kamon-system-metrics" % "1.0.0",
      "io.kamon" %% "kamon-http4s" % "1.0.8",
      "com.auth0" % "java-jwt" % "3.8.0"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),
   ).enablePlugins(JavaAppPackaging)


  lazy val siteSettings = Seq(
    micrositeName := "FoundaML",
    micrositeDescription := "Pipeline for machine learning algorithms",
    micrositeAuthor := "FoundaML contributors",
    micrositeOrganizationHomepage := "https://github.com/foundaml/server",
    micrositeGitterChannelUrl := "foundaml/server",
    micrositeGithubOwner := "foundaml",
    micrositeGithubRepo := "server",
    micrositeFavicons := Seq(
      microsites.MicrositeFavicon("favicon.png", "512x512")
    ),
    micrositeBaseUrl := "/server"
  )
  lazy val microsite = project.in(file("site"))
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(SiteScaladocPlugin)
  .settings(siteSettings)
  .dependsOn(app)
