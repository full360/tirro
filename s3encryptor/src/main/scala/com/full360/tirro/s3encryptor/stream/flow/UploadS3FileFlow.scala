package com.full360.tirro.s3encryptor.stream.flow

import java.io.File

import akka.event.LoggingAdapter
import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import com.full360.tirro.s3encryptor.config.Config
import com.google.inject.{ Inject, Singleton }

import scala.util.{ Failure, Success, Try }
import akka.stream.scaladsl.GraphDSL.Implicits._

@Singleton
class UploadS3FileFlow @Inject() (s3: AmazonS3, config: Config, log: LoggingAdapter) {

  def getFlow =
    Flow.fromGraph(getGraph)

  def getGraph =
    GraphDSL.create() { implicit b =>

      val uploadFile = b.add(Flow[File].map { file =>
        log.info(s"Uploading ${file.getName} to Bucket: ${config.s3BucketUpload}")
        val fileName = config.s3BucketPrefix + file.getName
        Try(s3.putObject(config.s3BucketUpload, fileName.trim, file))
      })

      val validUpload = b.add(Flow[Try[PutObjectResult]].map {
        case Success(e) =>
          log.info(s"File uploaded successfully... ${e.getETag}")
          Some(e)
        case Failure(ex) =>
          log.error(s"Cannot upload file got: ${ex.getMessage}")
          None
      })

      uploadFile ~> validUpload
      FlowShape(uploadFile.in, validUpload.out)
    }

}
