package com.full360.tirro.s3ftp.domain

import akka.util.ByteString

case class UserIdentity(
    principalId: String
)

case class RequestParameters(
    sourceIPAddress: String
)

case class Record(
    eventVersion:      String,
    eventSource:       String,
    awsRegion:         String,
    eventTime:         String,
    eventName:         String,
    userIdentity:      UserIdentity,
    requestParameters: RequestParameters,
    s3:                S3Event
)

case class S3Event(
    s3SchemaVersion: String,
    configurationId: String,
    bucket:          Bucket,
    `object`:        S3Object
)

case class S3Object(
    key:       String,
    size:      Long,
    eTag:      String,
    sequencer: String
)

case class Bucket(
    name:          String,
    ownerIdentity: UserIdentity,
    arn:           String
)

case class S3ContentFileName(
    content:  ByteString,
    filename: String
)