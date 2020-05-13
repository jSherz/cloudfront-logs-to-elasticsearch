name := "cloudfront-logs-to-elasticsaerch"

version := "0.1"

scalaVersion := "2.13.2"

val AkkaVersion = "2.6.5"
val AkkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "2.0.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % "2.0.0",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % AkkaHttpVersion,
  "software.amazon.awssdk" % "s3" % "2.11.3",
  "org.scalactic" %% "scalactic" % "3.1.1",
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-explaintypes",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Wdead-code",
  "-Wextra-implicit",
  "-Wnumeric-widen",
  "-Woctal-literal",
  "-Wconf:msg=Octal:s",
  "-Wself-implicit",
  "-Wunused",
  "-language:postfixOps"
)

scalastyleFailOnWarning := true

coverageEnabled := true
