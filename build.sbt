name := "callbacks"
version := "1.0"
scalaVersion := "2.11.8"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
    "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.0",
    "com.typesafe" % "config" % "1.3.0"
)
