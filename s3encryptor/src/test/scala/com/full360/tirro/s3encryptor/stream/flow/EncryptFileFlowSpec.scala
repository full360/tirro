package com.full360.tirro.s3encryptor.stream.flow

import java.io.File

import akka.actor.ActorSystem
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestKit
import com.full360.tirro.s3encryptor.client.integration.ClientIntegrationSpec
import com.full360.tirro.s3encryptor.utils.PGPUtils
import org.mockito.Matchers.{ anyBoolean, anyString }
import org.mockito.Mockito.doNothing
import com.full360.tirro.s3encryptor.utils.Utils.createTempFile
class EncryptFileFlowSpec extends TestKit(ActorSystem("EncryptFlowSpec")) with ClientIntegrationSpec {

  "A EncryptFlow" should {
    "Encrypt file with key" in {
      //Given
      val decryptor = mock[PGPUtils]

      doNothing().when(decryptor).encryptFile(anyString, anyString, anyString, anyBoolean, anyBoolean)

      val input: java.io.InputStream = new java.io.ByteArrayInputStream("test content".getBytes(java.nio.charset.StandardCharsets.UTF_8.name))

      val file = createTempFile("Test-Encrypt-File", input)

      val c = config.copy(publicKey = "myPublicKey")

      val flowUnderTest = new EncryptFileFlow(decryptor, c)

      //When
      val (scr, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[File], TestSink.probe[File])

      //Then
      scr
        .sendNext(file)
        .sendComplete()

      val result = sink
        .request(2)
        .expectNext()

      result.getName.contains(".pgp") shouldBe true

      sink.expectComplete()

    }
    "throw error is public key was not provided" in {
      //Given
      val decryptor = mock[PGPUtils]
      val file = new File("File")
      val c = config.copy(publicKey = "")
      val flowUnderTest = new EncryptFileFlow(decryptor, c)

      //When
      val (scr, sink) = flowUnderTest
        .getFlow
        .runWith(TestSource.probe[File], TestSink.probe[File])

      //Then
      scr
        .sendNext(file)
        .sendComplete()

      sink
        .request(2)
        .expectError()
    }
  }

}
