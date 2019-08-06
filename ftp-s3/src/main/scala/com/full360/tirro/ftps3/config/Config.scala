package com.full360.tirro.ftps3.config

final case class Config(
    ftpHost:          String = scala.util.Properties.envOrElse("FTP_ADDRESS", "localhost"),
    ftpPort:          Int    = scala.util.Properties.envOrElse("FTP_PORT", "22").toInt,
    ftpUser:          String = scala.util.Properties.envOrElse("FTP_USER", ""),
    ftpPass:          String = scala.util.Properties.envOrElse("FTP_PASSWORD", ""),
    ftpFileFilter:    String = "",
    ftpFileExtFilter: String = "",
    ftpSourcePath:    String = "",
    s3BucketName:     String = "dev.playground.hub.events.tf",
    var s3BucketKey:  String = "tirro",
    filter:           Int    = 0,
    loglevel:         String = "DEBUG",
    continous:        Int    = 0
)
