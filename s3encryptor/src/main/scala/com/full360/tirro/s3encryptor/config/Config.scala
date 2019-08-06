package com.full360.tirro.s3encryptor.config

case class Config(
    s3BucketUpload: String = "dev.f360.s3.tests",
    s3BucketPrefix: String = "To-C2/",
    loglevel:       String = "DEBUG",
    sqsQueue:       String = "https://sqs.us-west-2.amazonaws.com/530886205474/test-hub",
    publicKey:      String = scala.util.Properties.envOrElse("GPG_PUBLIC_KEY", "")
)
