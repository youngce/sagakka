name := "sagakka"

version := "0.1"

scalaVersion := "2.12.8"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-persistence
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.21"
// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.7" % Test
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-testkit
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.21" % Test

resolvers += Resolver.jcenterRepo

// https://mvnrepository.com/artifact/com.github.dnvriend/akka-persistence-inmemory
libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1" % Test

