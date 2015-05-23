import sbt._
import sbt.Keys._
import scala.collection.JavaConversions._
import Dependencies._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._

object Build extends Build {
  val AppVersion = System.getenv().getOrElse("GO_PIPELINE_LABEL", "1.0.0-SNAPSHOT")
  val ScalaVersion = "2.10.4"

  val main = Project("vamana", file("."),
    settings = defaultSettings ++ Seq(
      organization := "in.ashwanthkumar",
      version := AppVersion,
      libraryDependencies ++= appDependencies
    ))

  lazy val appDependencies = Seq(
    hadoopCore, hadoopClient, jcloudsCore, finatra, typeSafeConfig,
    // test deps
    scalaTest, mockito
  )

  lazy val defaultSettings = super.settings ++ defaultAssemblySettings ++ Seq(
    fork in run := false,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "Cloudera Resolver" at "https://repository.cloudera.com/cloudera/cloudera-repos/",
    resolvers += "Twitter Maven Repo" at "http://maven.twttr.com/",
    parallelExecution in This := false,
    publishMavenStyle := true,
    crossPaths := true,
    publishArtifact in Test := false,
    publishArtifact in(Compile, packageDoc) := false,
    // publishing the main sources jar
    publishArtifact in(Compile, packageSrc) := true,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )

  lazy val defaultAssemblySettings = assemblySettings ++ Seq(
    mainClass in assembly := None,
    packageOptions in assembly ~= { os => os filterNot {
      _.isInstanceOf[Package.MainClass]
    }},
    mergeStrategy in assembly := defaultMergeStrategy,
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)
  )

  val defaultMergeStrategy: String => MergeStrategy = {
    case "reference.conf" | "rootdoc.txt" =>
      MergeStrategy.concat
    case PathList("META-INF", xs@_*) =>
      (xs map {
        _.toLowerCase
      }) match {
        case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
          MergeStrategy.discard
        case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
          MergeStrategy.discard
        case "plexus" :: xs =>
          MergeStrategy.discard
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case ("mailcap.default" :: Nil) | ("mimetypes.default" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case ("javamail.default.providers" :: Nil) | ("mailcap" :: Nil) | ("javamail.default.address.amp" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case ("javamail.charset.map" :: Nil) | ("gfprobe-provider.xml" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.discard
      }
    case _ => MergeStrategy.first
  }

}

object Dependencies {
  val hadoopCore = "org.apache.hadoop" % "hadoop-core" % "2.0.0-mr1-cdh4.2.1" % "provided"
  val hadoopClient = "org.apache.hadoop" % "hadoop-client" % "2.0.0-mr1-cdh4.2.1" % "provided"
  val finatra = "com.twitter" %% "finatra" % "1.6.0"
  val jcloudsCore = "org.apache.jclouds" % "jclouds-all" % "1.9.0"
  val typeSafeConfig = "com.typesafe" % "config" % "1.2.1"


  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.0" % "test"
  val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
}
