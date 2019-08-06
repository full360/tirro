package com.full360.tirro.s3encryptor.stream.flow

import java.io.File

import akka.event.LoggingAdapter
import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.full360.tirro.s3encryptor.config.Config
import com.google.inject.{ Inject, Singleton }
import com.full360.tirro.s3encryptor.utils.Utils.{ parseToRecord, removeS3OriginalPath, createTempFile }
import akka.stream.scaladsl.GraphDSL.Implicits._
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.full360.tirro.s3encryptor.domain.Record
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import scala.util.{ Failure, Success, Try }

@Singleton
class DownloadS3FileFlow @Inject() (config: Config, logging: LoggingAdapter, s3: AmazonS3, sqsClient: SqsAsyncClient) {

  type SQSBodyWithReceiptHandle = (String, String)

  implicit val log = logging

  implicit val sqs = sqsClient

  def getFlow =
    Flow.fromGraph(getGraph)

  def getGraph =
    GraphDSL.create() { implicit b =>

      val getBody = b.add(Flow[Message].map(msg => (msg.body(), msg.receiptHandle())).log("before-parse"))

      val getS3Key = b.add(Flow[SQSBodyWithReceiptHandle].map(element => parseToRecord(element._1, element._2, config.sqsQueue)))

      val filterValidRecords = b.add(Flow[Option[List[Record]]].filter(_.isDefined).map(_.get))

      val expandFlow = b.add(Flow[List[Record]].mapConcat(element => element))

      val downloadS3File = b.add(Flow[Record].map { s3Object =>
        Try(s3.getObject(s3Object.s3.bucket.name, s3Object.s3.`object`.key))
      })

      val writeIntoFile = b.add(Flow[Try[S3Object]].map {
        case Success(s3object) =>
          val fileName = removeS3OriginalPath(s3object.getKey)
          Some(createTempFile(fileName, s3object.getObjectContent))
        case Failure(ex) =>
          log.error(s"Cannot download file from S3 error: ${ex.getMessage}")
          None
      })

      val getValidFile = b.add(Flow[Option[File]].filter(_.isDefined).map(_.get))

      getBody ~> getS3Key ~> filterValidRecords ~> expandFlow ~> downloadS3File ~> writeIntoFile ~> getValidFile
      FlowShape(getBody.in, getValidFile.out)
    }

}
