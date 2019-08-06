import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object Settings {
  lazy val common = Seq(
    organization := "com.full360",
    organizationName := "Full 360 Inc.",
    scalaVersion := "2.12.8",
    crossVersion := CrossVersion.binary,
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignArguments, true)
      .setPreference(RewriteArrowSymbols, false)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(SpacesAroundMultiImports, true)
      .setPreference(DanglingCloseParenthesis, Force)
      .setPreference(FirstArgumentOnNewline, Force)
      .setPreference(FirstParameterOnNewline, Force),
    scalacOptions := Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-target:jvm-1.8",
      "-unchecked",
      "-Xfuture",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code"),
    shellPrompt := { s => s"${Project.extract(s).currentProject.id} > " })

  lazy val noPackaging = Seq(
    Keys.`package` :=  file(""),
    packageBin in Global :=  file(""),
    packagedArtifacts :=  Map()
  )

}

