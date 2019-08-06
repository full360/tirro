package com.full360.tirro.s3ftp.config

final case class Config(
    sqsQueue:      String = "https://sqs.ap-southeast-2.amazonaws.com/115159323386/ql-dev-hub-card-raw-events",
    ftpHost:       String = scala.util.Properties.envOrElse("FTP_ADDRESS", "localhost"),
    ftpPort:       Int    = scala.util.Properties.envOrElse("FTP_PORT", "22").toInt,
    ftpUser:       String = scala.util.Properties.envOrElse("FTP_USER", ""),
    ftpPass:       String = scala.util.Properties.envOrElse("FTP_PASSWORD", ""),
    ftpSinkPath:   String = "/Users/eduardo/Upload/",
    var bucketKey: String = "cosa.txt"
)
