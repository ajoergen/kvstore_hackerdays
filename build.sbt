name := """NGrams"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  //jdbc,
  //anorm,
  cache,
  //ws,
  // Adding dependencies to akka actors and webjars.
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.3",
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "bootstrap" % "3.1.1-1",
  "org.webjars" % "jquery" % "1.11.1",
  //"com.yammer.metrics" % "metrics-core" % "2.1.2",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test"
)

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")