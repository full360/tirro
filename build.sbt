lazy val root = (project in file("."))
  .enablePlugins(PackPlugin)
  .settings(Settings.noPackaging)
  .settings(
    onLoadMessage :=
      """
        |** Welcome to the sbt build definition for Tirro **
        |
        |Useful sbt tasks:
        |- project <name-of-the-project>
        |- test
      """.stripMargin
  )
  .aggregate(ftpS3,s3Ftp,s3Encryptor)
  .dependsOn(ftpS3,s3Ftp,s3Encryptor)


lazy val ftpS3 = Project("ftp-s3", file("ftp-s3"))
  .settings(Settings.common)
  .settings(Dependencies.FtpToS3)
  .settings(
    name := "tirro-ftp-s3",
    parallelExecution in Test := false,
    fork in Test := true
  )

lazy val s3Encryptor = project
  .in(file("s3encryptor"))
  .settings(Settings.common)
  .settings(Dependencies.s3Encryptor)
  .settings(
    name := "s3-Encryptor",
    parallelExecution in Test := false,
    fork in Test := true
  )

lazy val s3Ftp = project
  .in(file("s3-ftp"))
  .settings(Settings.common)
  .settings(Dependencies.FtpToS3)
  .settings(
    name := "tirro-s3-ftp",
    parallelExecution in Test := false,
    fork in Test := true
  )

lazy val s3Redshift = project
  .in(file("s3-redshift"))
  .settings(
    name := "tirro-s3-redshift",
    parallelExecution in Test := false,
    fork in Test := true
  )
