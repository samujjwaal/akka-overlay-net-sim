name := "overlaynetworksimulator"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.typesafe" % "config" % "1.4.0",
  "org.cloudsimplus" % "cloudsim-plus" % "5.4.3",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "junit" % "junit" % "4.12" % "test",
)
