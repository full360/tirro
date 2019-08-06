package com.full360.tirro.s3encryptor.stream.flow

import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.amazonaws.services.s3.model.PutObjectResult
import com.full360.tirro.s3encryptor.config.Config
import com.google.inject.{ Inject, Singleton }
import akka.stream.scaladsl.GraphDSL.Implicits._
import com.full360.tirro.s3encryptor.utils.Utils.deleteMessageFromSQS
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message

@Singleton
class DeleteSQSMsgFlow @Inject() (sqs: SqsAsyncClient, config: Config) {

  def getFlow =
    Flow.fromGraph(getGraph)

  def getGraph =
    GraphDSL.create() { implicit b =>

      val filterSuccessMessage = b.add(Flow[(Option[PutObjectResult], Message)].filter(_._1.isDefined))

      val deleteMessageFromQueue = b.add(Flow[(Option[PutObjectResult], Message)].map { tupla =>
        deleteMessageFromSQS(config.sqsQueue, tupla._2.receiptHandle())(sqs)
      })

      filterSuccessMessage ~> deleteMessageFromQueue
      FlowShape(filterSuccessMessage.in, deleteMessageFromQueue.out)
    }

}
