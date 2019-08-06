package com.full360.tirro.s3encryptor.stream

import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ Broadcast, GraphDSL, RunnableGraph, Sink, Source, Zip }
import com.google.inject.Inject
import akka.stream.scaladsl.GraphDSL.Implicits._
import com.amazonaws.services.s3.model.PutObjectResult
import com.full360.tirro.s3encryptor.stream.flow.{ DeleteSQSMsgFlow, DownloadS3FileFlow, EncryptFileFlow, UploadS3FileFlow }
import software.amazon.awssdk.services.sqs.model.Message

class S3EncryptorStream @Inject() (
    source:             Source[Message, NotUsed],
    downloadS3FileFlow: DownloadS3FileFlow,
    encryptFileFlow:    EncryptFileFlow,
    uploadS3FileFlow:   UploadS3FileFlow,
    deleteSQSMsgFlow:   DeleteSQSMsgFlow
) {

  def getStream =
    RunnableGraph.fromGraph(getGraph)

  def getGraph =
    GraphDSL.create(Sink.ignore) { implicit b => sink =>

      //SQS SRC
      val sqsSrc = b.add(source).out.log("after-SQS")

      // GET S3 FILE
      val downloadS3File = downloadS3FileFlow.getFlow.log("after-download")
      // ENCRYPT
      val encryptFlow = encryptFileFlow.getFlow.log("after-encrypt")
      // UPLOAD FILE
      val uploadS3File = uploadS3FileFlow.getFlow

      val zipSuccess = b.add(Zip[Option[PutObjectResult], Message])

      val broadcast = b.add(Broadcast[Message](2))

      val deleteSuccessMessages = deleteSQSMsgFlow.getFlow

      sqsSrc ~> broadcast.in

      broadcast.out(0) ~> downloadS3File ~> encryptFlow ~> uploadS3File ~> zipSuccess.in0
      broadcast.out(1) ~> zipSuccess.in1

      zipSuccess.out ~> deleteSuccessMessages ~> sink
      ClosedShape
    }
}
