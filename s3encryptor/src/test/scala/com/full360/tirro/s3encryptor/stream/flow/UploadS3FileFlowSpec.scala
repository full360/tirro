package com.full360.tirro.s3encryptor.stream.flow

import java.io.File

import akka.actor.ActorSystem
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestKit
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.{ AWSStaticCredentialsProvider, AnonymousAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.amazonaws.services.s3.model.PutObjectResult
import com.full360.tirro.s3encryptor.client.integration.ClientIntegrationSpec
import com.full360.tirro.s3encryptor.utils.Utils.createTempFile
import io.findify.s3mock.S3Mock
import org.mockito.Mockito.when
import org.mockito.Matchers.{ anyString, any }

import scala.collection.JavaConverters._

class UploadS3FileFlowSpec extends TestKit(ActorSystem("UploadS3FileFlowSpec")) with ClientIntegrationSpec {

  "A UploadS3FileFlow" should {
    "Upload File To S3 successfully" in {
      //Given
      val api = S3Mock(port = 8002, dir = "/tmp/s3")
      api.start

      val endpoint = new EndpointConfiguration("http://localhost:8002", "us-west-2")
      val s3 = AmazonS3ClientBuilder
        .standard
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(endpoint)
        .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
        .build

      s3.createBucket(config.s3BucketUpload)

      val input: java.io.InputStream = new java.io.ByteArrayInputStream("test content".getBytes(java.nio.charset.StandardCharsets.UTF_8.name))

      val file = createTempFile("Test-File", input)

      val s3ObjectKey = config.s3BucketPrefix + "Test-File"

      val flowUnderTest = new UploadS3FileFlow(s3, config, logger)

      //When
      val (src, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[File], TestSink.probe[Option[PutObjectResult]])

      //Then
      src
        .sendNext(file)
        .sendComplete()

      sink
        .request(2)
        .expectNext()

      //Verify s3 client Uploaded the file successfully
      val objects = s3.listObjects(config.s3BucketUpload).getObjectSummaries.asScala
      val obj = objects.filter(_.getKey == s3ObjectKey)
      obj.size shouldBe 1
      obj.head.getKey shouldBe s3ObjectKey

      sink.expectComplete()
    }
  }
  "Drop item if cannot upload file to s3" in {
    //Given
    val s3 = mock[AmazonS3]
    val ex = new AmazonServiceException("BOOM")

    when(s3.putObject(anyString, anyString, any[File])) thenThrow ex

    val file = new File("Bad-File")

    val flowUnderTest = new UploadS3FileFlow(s3, config, logger)

    //When

    val (src, sink) = flowUnderTest
      .getFlow
      .runWith(TestSource.probe[File], TestSink.probe[Option[PutObjectResult]])

    //Then
    src
      .sendNext(file)
      .sendComplete()

    sink
      .request(2)
      .expectNext(None)
      .expectComplete()
  }

}
