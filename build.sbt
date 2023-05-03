ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "Scala_VB"
  )

val AkkaVersion = "2.8.0"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor-typed
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion

// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.7"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.7"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-cluster-typed
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion
