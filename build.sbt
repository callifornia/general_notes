lazy val root = (project in file(".")).settings(name := "poligon")
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.8"
val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.9"
val CatsEffectVersion = "3.3.12"
val ZioVersion = "1.0.12"



libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "de.svenkubiak" % "jBCrypt" % "0.4",
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.lightbend.akka" %% "akka-projection-core" % "1.2.4",
  "com.lightbend.akka" %% "akka-projection-eventsourced" % "1.2.4",
  "com.lightbend.akka" %% "akka-projection-cassandra" % "1.2.4",

  "org.typelevel" %% "cats-effect" % CatsEffectVersion,
  "org.typelevel" %% "cats-core" % "2.8.0",
  "org.typelevel" %% "cats-free" % "2.8.0",
  "dev.zio" %% "zio" % ZioVersion,

)


