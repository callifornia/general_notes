lazy val root = (project in file(".")).settings(name := "poligon")
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.8"
val AkkaVersion = "2.6.19"



libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
)


