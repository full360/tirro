package com.full360.tirro.s3ftp.module

import com.google.inject.{ AbstractModule, Provides, Singleton }
import net.codingwell.scalaguice.ScalaModule
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.Future
import akka.NotUsed
import akka.util.ByteString
import com.full360.tirro.s3ftp.config.{ Config, ConfigParser }
import akka.stream.alpakka.ftp.scaladsl.Sftp
import akka.stream.IOResult
import akka.stream.alpakka.sqs.scaladsl.{ SqsAckSink, SqsSource }
import com.amazonaws.auth.{ AWSCredentialsProvider, DefaultAWSCredentialsProviderChain }
import com.amazonaws.regions.{ AwsRegionProvider, DefaultAwsRegionProviderChain }
import akka.stream.alpakka.sqs.{ MessageAction, SqsSourceSettings }
import akka.stream.alpakka.ftp._
import java.net.InetAddress

import akka.event.LoggingAdapter
import akka.stream.alpakka.s3.scaladsl.S3
import com.full360.tirro.s3ftp.Utils.removeS3OriginalPath
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.ExecutionContext.Implicits.global

class Module(args: Array[String], system: ActorSystem, materializer: ActorMaterializer, logging: LoggingAdapter) extends AbstractModule with ScalaModule {

  override def configure() = Unit

  @Provides
  @Singleton
  def provideConfig() = {
    new ConfigParser().parse(args, Config()) match {
      case Some(config) ⇒ config
      case None         ⇒ throw new RuntimeException("Cannot parse arguments")
    }
  }

  @Provides
  @Singleton
  def providesAWSCredentialsProvider: AWSCredentialsProvider = {
    DefaultAWSCredentialsProviderChain.getInstance()
  }

  @Provides
  @Singleton
  def providesAWSRegionProvider: AwsRegionProvider = {
    new DefaultAwsRegionProviderChain()
  }

  @Provides
  @Singleton
  def providesSQSAsyncClient: SqsAsyncClient = SqsAsyncClient.create()

  @Provides
  @Singleton
  def provideSFTPSettings(config: Config): SftpSettings = {
    val credentials = FtpCredentials.create(config.ftpUser, config.ftpPass)
    SftpSettings(InetAddress.getByName(config.ftpHost))
      .withPort(config.ftpPort)
      .withCredentials(credentials)
      .withStrictHostKeyChecking(false)
  }

  @Provides
  @Singleton
  def provideSFtpSinkconfig(config: Config, settings: SftpSettings): Sink[ByteString, Future[Future[IOResult]]] = {
    Sink.lazyInit[ByteString, Future[IOResult]](
      _ ⇒ {
        val fileName = removeS3OriginalPath(config.bucketKey)
        logging.info("Uploading to: " + config.ftpSinkPath + " The file  " + fileName)
        Future(Sftp.toPath(config.ftpSinkPath + fileName, settings, false))
      },
      () => Future(IOResult.createSuccessful(0))
    )
  }

  @Provides
  @Singleton
  def providesSQSSourceSettings: SqsSourceSettings =
    SqsSourceSettings()

  @Provides
  @Singleton
  def provideSource(sqsClient: SqsAsyncClient, config: Config, settings: SqsSourceSettings) = {
    SqsSource(config.sqsQueue, settings)(sqsClient)
      .take(1)
      .map(MessageAction.Delete(_))
      .alsoTo(SqsAckSink(config.sqsQueue)(sqsClient))
      .map(_.message)
  }

  @Provides
  @Singleton
  def provideFlow(config: Config): Flow[Message, ByteString, NotUsed] = {
    Flow[Message].flatMapConcat({ item =>
      com.full360.tirro.s3ftp.Utils.parseToRecord(item.body()) match {
        case Some(evt) =>
          val event = evt.head
          config.bucketKey = event.s3.`object`.key
          S3.download(event.s3.bucket.name, event.s3.`object`.key)
        case None =>
          logging.warning("invalid json")
          Source.empty
      }
    }).log("middle").filter(_.isDefined).flatMapConcat(_.get._1)
  }
}
