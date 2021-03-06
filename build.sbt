name := "group11_project"

version := "0.1"

scalaVersion := "2.13.3"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor-typed
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.typesafe.akka" %% "akka-actor" % "2.6.10",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.10",
  "com.typesafe.akka" %% "akka-stream" % "2.6.10",
  "com.typesafe.akka" %% "akka-http" % "10.2.1",
  "com.typesafe.akka" %% "akka-cluster-typed" % "2.6.10",
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" %"2.6.10",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "com.google.code.gson" % "gson" % "2.8.5",
  "commons-io" % "commons-io" % "2.6",
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",
  "org.scalactic" %% "scalactic" % "3.2.2",
  "com.typesafe" % "config" % "1.4.0"
)

mainClass in(Compile, run) := Some("com.group11.Main")
