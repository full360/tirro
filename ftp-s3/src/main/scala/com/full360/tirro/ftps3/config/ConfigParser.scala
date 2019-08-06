package com.full360.tirro.ftps3.config

import com.google.inject.{ Inject, Singleton }

@Singleton
class ConfigParser @Inject() () extends scopt.OptionParser[Config]("tirro-ftp-s3") {

  val default = Config()

  help("help")
    .text("Print this usage text")

  // Ftp
  opt[String]("ftp-user")
    .valueName("<username>")
    .action((str, opt) ⇒ opt.copy(ftpUser = str))
    .text("User to connect to FTP. Required")

  opt[String]("ftp-password")
    .valueName("<password>")
    .action((str, opt) ⇒ opt.copy(ftpPass = str))
    .text("Password to connect to FTP. Required")

  opt[String]("ftp-adr")
    .valueName("<address>")
    .action((str, opt) ⇒ opt.copy(ftpHost = str))
    .text(s"Address to connect to FTP. Default ${default.ftpHost}")

  opt[Int]("ftp-port")
    .valueName("<port>")
    .action((num, opt) ⇒ opt.copy(ftpPort = num))
    .text(s"Port to connect to FTP. Default ${default.ftpPort}")

  opt[String]("ftp-file-path")
    .valueName("<path>")
    .action((str, opt) ⇒ opt.copy(ftpSourcePath = str))
    .text("Remote destination path. Optional")

  opt[String]("bucket-name")
    .valueName("<path>")
    .action((str, opt) ⇒ opt.copy(s3BucketName = str))

  opt[String]("bucket-path")
    .valueName("<path>")
    .action((str, opt) ⇒ opt.copy(s3BucketKey = str))

  opt[String]("ftp-file-filter")
    .valueName("<ftpFileFilter>")
    .action((str, opt) ⇒ opt.copy(ftpFileFilter = str))

  opt[String]("ftp-File-ext-filter")
    .valueName("<ftpFileExtFilter>")
    .action((str, opt) ⇒ opt.copy(ftpFileExtFilter = str))

  opt[Int]("filter")
    .valueName("<filter>")
    .action((int, opt) ⇒ opt.copy(filter = int))

  opt[Int]("continous")
    .valueName("<continous>")
    .action((int, opt) ⇒ opt.copy(continous = int))

  opt[String]("log-level")
    .valueName("<loglevel>")
    .action((str, opt) ⇒ opt.copy(loglevel = str))
}
