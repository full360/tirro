package com.full360.tirro.s3encryptor.stream.flow

import akka.actor.ActorSystem
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestKit
import com.amazonaws.services.s3.model.PutObjectResult
import com.full360.tirro.s3encryptor.client.integration.ClientIntegrationSpec
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{ DeleteMessageRequest, DeleteMessageResponse, Message }

import scala.concurrent.Future
import scala.compat.java8.FutureConverters._

class DeleteSQSMsgFlowSpec extends TestKit(ActorSystem("DeleteSQSMsgSystem")) with ClientIntegrationSpec {

  "A DeleteSQSMsg" should {
    "Delete message from SQS" in {
      //Given
      val sqs = mock[SqsAsyncClient]
      val delete = DeleteMessageResponse.builder().build()

      val message = Message.builder().receiptHandle("Receipt").build()

      val result = new PutObjectResult()
      when(sqs.deleteMessage(any[DeleteMessageRequest])) thenReturn Future.apply(delete).toJava.toCompletableFuture

      val flowUnderTest = new DeleteSQSMsgFlow(sqs, config)

      //When
      val (src, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[(Option[PutObjectResult], Message)], TestSink.probe)

      //Then
      src
        .sendNext((Some(result), message))
        .sendComplete()

      sink
        .request(2)
        .expectNext()

      sink.expectComplete()
    }
    "Drop message if is not a successful result" in {
      //Given
      val sqs = mock[SqsAsyncClient]

      val message = Message.builder().build()

      val flowUnderTest = new DeleteSQSMsgFlow(sqs, config)

      //When
      val (src, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[(Option[PutObjectResult], Message)], TestSink.probe)

      //Then
      src
        .sendNext((None, message))
        .sendComplete()

      sink
        .request(2)
        .expectComplete()

    }
  }

}
