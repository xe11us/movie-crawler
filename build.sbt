name := "coursework"

version := "0.1"

scalaVersion := "2.13.5"

lazy val circeVersion = "0.13.0"
lazy val tapirVersion = "0.18.0-M4"

ThisBuild / libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,

  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,

  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.softwaremill.macwire" %% "macros" % "2.3.7",
  "com.beachape" %% "enumeratum" % "1.6.1",
  "com.beachape" %% "enumeratum-circe" % "1.6.1",

  "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.13" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.2.4" % Test,
  "org.scalamock" %% "scalamock" % "4.4.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.2" % Test,
  "org.json4s" %% "json4s-native" % "3.7.0-M16"
)

enablePlugins(ScalaJSPlugin)
scalaJSUseMainModuleInitializer := true

ThisBuild / Test / parallelExecution := false