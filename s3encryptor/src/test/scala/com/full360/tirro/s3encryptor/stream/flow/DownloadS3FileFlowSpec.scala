package com.full360.tirro.s3encryptor.stream.flow

import java.io.File

import akka.actor.ActorSystem
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestKit
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.{ AWSStaticCredentialsProvider, AnonymousAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.full360.tirro.s3encryptor.client.integration.ClientIntegrationSpec
import io.findify.s3mock.S3Mock
import org.mockito.Matchers.{ any, anyString }
import org.mockito.Mockito.when
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{ DeleteMessageRequest, DeleteMessageResponse, Message }

import scala.compat.java8.FutureConverters._
import scala.concurrent.Future

class DownloadS3FileFlowSpec extends TestKit(ActorSystem("DownloadS3FileSystem")) with ClientIntegrationSpec {

  "A DownloadS3File" should {
    "Download file from S3" in {
      //Given
      val sqs = mock[SqsAsyncClient]

      val api = S3Mock(port = 8001, dir = "/tmp/s3")
      api.start

      val endpoint = new EndpointConfiguration("http://localhost:8001", "us-west-2")
      val s3 = AmazonS3ClientBuilder
        .standard
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(endpoint)
        .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
        .build

      s3.createBucket("dev.bckt")
      s3.putObject("dev.bckt", "test-key", "Content")

      val body = """{ "Records": [ { "eventVersion": "2.0", "eventSource": "aws:s3", "awsRegion": "us-west-2", "eventTime": "2018-06-27T15:05:27.677Z", "eventName": "ObjectCreated:Put", "userIdentity": { "principalId": "AWS:asdfasdfasdfasdf" }, "requestParameters": { "sourceIPAddress": "192.168.0.0" }, "responseElements": { "x-amz-request-id": "12341234123", "x-amz-id-2": "asdfjioadifosjdf" }, "s3": { "s3SchemaVersion": "1.0", "configurationId": "PutNewFileTest", "bucket": { "name": "dev.bckt", "ownerIdentity": { "principalId": "123123" }, "arn": "arn:aws:s3:::dev.tests" }, "object": { "key": "test-key", "size": 564, "eTag": "123123123", "sequencer": "1231231" } } } ] }"""

      val message = Message.builder().body(body).build()

      val flowUnderTest = new DownloadS3FileFlow(config, logger, s3, sqs)

      //When
      val (src, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[Message], TestSink.probe[File])

      //Then
      src
        .sendNext(message)
        .sendComplete()

      val element = sink
        .request(2)
        .expectNext()

      element.getName shouldBe "test-key"

      sink.expectComplete()
    }
    "Drop item if cannot download file from s3" in {
      //Given
      val sqs = mock[SqsAsyncClient]

      val s3 = mock[AmazonS3]
      val ex = new AmazonServiceException("BOOM")
      when(s3.getObject(anyString, anyString)) thenThrow ex

      val message = Message.builder().body("""{ "Records": [ { "eventVersion": "2.0", "eventSource": "aws:s3", "awsRegion": "us-west-2", "eventTime": "2018-06-27T15:05:27.677Z", "eventName": "ObjectCreated:Put", "userIdentity": { "principalId": "AWS:asdfasdfasdfasdf" }, "requestParameters": { "sourceIPAddress": "192.168.0.0" }, "responseElements": { "x-amz-request-id": "12341234123", "x-amz-id-2": "asdfjioadifosjdf" }, "s3": { "s3SchemaVersion": "1.0", "configurationId": "PutNewFileTest", "bucket": { "name": "dev.bckt", "ownerIdentity": { "principalId": "123123" }, "arn": "arn:aws:s3:::dev.tests" }, "object": { "key": "test-key", "size": 564, "eTag": "123123123", "sequencer": "1231231" } } } ] }""")
        .build()

      val flowUnderTest = new DownloadS3FileFlow(config, logger, s3, sqs)

      //When
      val (src, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[Message], TestSink.probe[File])

      //Then
      src
        .sendNext(message)
        .sendComplete()

      sink
        .request(2)
        .expectComplete()
    }
    "Drop item if cannot parse sqs message" in {
      //Given
      val sqs = mock[SqsAsyncClient]
      val delete = DeleteMessageResponse.builder().build()

      when(sqs.deleteMessage(any[DeleteMessageRequest])) thenReturn Future.apply(delete).toJava.toCompletableFuture

      val s3 = mock[AmazonS3]
      val ex = new AmazonServiceException("BOOM")
      when(s3.getObject(anyString, anyString)) thenThrow ex

      val message = Message.builder.body("BAD-MSG").build()

      val flowUnderTest = new DownloadS3FileFlow(config, logger, s3, sqs)

      //When
      val (src, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[Message], TestSink.probe[File])

      //Then
      src
        .sendNext(message)
        .sendComplete()

      sink
        .request(2)
        .expectComplete()
    }
  }

}
