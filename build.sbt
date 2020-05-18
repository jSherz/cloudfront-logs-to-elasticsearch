import org.apache.ivy.core.module.descriptor.ExcludeRule

name := "cloudfront-logs-to-elasticsearch"

version := "1.0"

scalaVersion := "2.13.2"

val AkkaVersion = "2.6.5"
val AkkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "2.0.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % "2.0.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "2.0.0" excludeAll (ExclusionRule(
    organization = "com.github.matsluni"
  )),
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
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

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") =>
    MergeStrategy.discard
  case PathList("module-info.class")     => MergeStrategy.discard
  case PathList("codegen-resources", _*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some(
  "com.jsherz.cloudfrontlogstoes.LogUploadNotificationProcessor"
)
