package com.full360.tirro.s3ftp.config

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
    .action((str, opt) ⇒ opt.copy(ftpSinkPath = str))
    .text("Remote destination path. Optional")

  //SQS
  opt[String]("sqs-url")
    .valueName("<sqs-url>")
    .action((str, opt) ⇒ opt.copy(sqsQueue = str))
    .text("URL for SQS Queue")
}
