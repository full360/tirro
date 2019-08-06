package com.full360.tirro.s3encryptor.utils

import java.io.{ File, InputStream }
import java.nio.file.StandardCopyOption

import akka.event.LoggingAdapter
import com.full360.tirro.s3encryptor.domain.Record

import scala.util.{ Failure, Success, Try }
import org.json4s._
import org.json4s.jackson.JsonMethods._
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest

object Utils {

  implicit val formats = org.json4s.DefaultFormats

  def parseToRecord(event: String, receiptHandle: String, queueURL: String)(implicit log: LoggingAdapter, sqsClient: SqsAsyncClient): Option[List[Record]] = {
    Try((parse(event) \ "Records").extract[List[Record]]) match {
      case Success(s3Key) if s3Key.nonEmpty => Some(s3Key)
      case Success(_) =>
        log.warning("no s3 key found it")
        deleteMessageFromSQS(queueURL, receiptHandle)
        None
      case Failure(e) =>
        deleteMessageFromSQS(queueURL, receiptHandle)
        log.error(e.getMessage)
        None
    }
  }

  def deleteMessageFromSQS(queueUrl: String, receiptHandle: String)(implicit sqsClient: SqsAsyncClient) =
    sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build()).join()

  def createTempFile(name: String, content: InputStream): File = {
    val file = new File(name)
    file.deleteOnExit()

    java.nio.file.Files.copy(
      content,
      file.toPath,
      StandardCopyOption.REPLACE_EXISTING
    )

    content.close()
    file
  }

  def createTempFile(name: String, ext: String): File = {
    val file = new File(name + ext)
    file.deleteOnExit()
    file
  }

  def removeS3OriginalPath(s3FileKey: String): String = {
    s3FileKey.split("/").last
  }

}
