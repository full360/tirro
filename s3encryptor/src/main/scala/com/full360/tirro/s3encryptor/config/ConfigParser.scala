package com.full360.tirro.s3encryptor.config

import com.google.inject.Inject

class ConfigParser @Inject() () extends scopt.OptionParser[Config]("tirro-s3-encryptor") {

  val default = Config()

  help("help")
    .text("Print this usage text")

  opt[String]("sqs-url")
    .valueName("<sqs-url>")
    .action((str, opt) ⇒ opt.copy(sqsQueue = str))
    .text("URL for SQS Queue")

  opt[String]("s3-bucket-upload")
    .valueName("<s3-bucket-upload>")
    .action((str, opt) => opt.copy(s3BucketUpload = str))
    .text("S3 Bucket to upload file")

  opt[String]("s3-bucket-prefix")
    .valueName("s3-bucket-prefix>")
    .action((str, opt) => opt.copy(s3BucketPrefix = str))
    .text("S3 Bucket Prefix ex: to-FTP/")

  opt[String]("log-level")
    .valueName("<log-level>")
    .action((str, opt) => opt.copy(loglevel = str))
    .text("Level for logging")
}
