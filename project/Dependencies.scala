import sbt._
import Keys._

object Dependencies {
  val alpakkaVersion = "1.1.0"
  val bouncyCastleVersion = "1.56"
  val akkaVersion = "2.5.23"

  val alpakkaFtp = "com.lightbend.akka" %% "akka-stream-alpakka-ftp" % alpakkaVersion
  val alpakkaS3 = "com.lightbend.akka" %% "akka-stream-alpakka-s3" % alpakkaVersion
  val alpakkaSQS = "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % alpakkaVersion
  val guice = "net.codingwell" %% "scala-guice" % "4.1.0"
  val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.184"
  val scopt = "com.github.scopt" %% "scopt" % "3.7.0"
  val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.5.4"
  val json4sNative = "org.json4s" %% "json4s-native" % "3.5.4"
  val bouncycastleCPG = "org.bouncycastle" % "bcpg-jdk15on" % bouncyCastleVersion
  val bouncycastleJC = "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion

  // ---------------------------------------------------------------------------
  // Dependencies for Tests
  // ---------------------------------------------------------------------------
  val mockitoAll = "org.mockito" % "mockito-all" % "1.10.19"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.0"
  val guiceTestLib = "com.google.inject.extensions" % "guice-testlib" % "4.1.0"
  val streamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val s3mock = "io.findify" %% "s3mock" % "0.2.4"

  val FtpToS3 = Seq(
    libraryDependencies ++= Seq(
      alpakkaFtp,
      alpakkaS3,
      alpakkaSQS,
      json4sJackson,
      json4sNative,
      guice,
      awsS3,
      scopt))

  val s3Encryptor = Seq(
    libraryDependencies ++= Seq(
      awsS3,
      alpakkaSQS,
      guice,
      scopt,
      json4sJackson,
      json4sNative,
      bouncycastleCPG,
      bouncycastleJC,
      mockitoAll,
      scalatest,
      guiceTestLib,
      streamTestkit,
      s3mock))
}
