package com.full360.tirro.s3encryptor.module

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sqs.SqsSourceSettings
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.scaladsl.Source
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.full360.tirro.s3encryptor.config.{ Config, ConfigParser }
import com.full360.tirro.s3encryptor.utils.PGPUtils
import com.google.inject.{ AbstractModule, Provides, Singleton }
import net.codingwell.scalaguice.ScalaModule
import com.typesafe.config.{ ConfigFactory, Config => typeSafe }
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success, Try }

class Module(args: Array[String]) extends AbstractModule with ScalaModule {

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
  def provideSource(sqsClient: SqsAsyncClient, config: Config, settings: SqsSourceSettings): Source[Message, NotUsed] =
    SqsSource(config.sqsQueue, settings)(sqsClient)

  @Provides
  @Singleton
  def provideConfigLog(config: Config): typeSafe = ConfigFactory.load(ConfigFactory.parseString(s"""akka{loglevel=${config.loglevel}}""").withFallback(ConfigFactory.load))

  @Provides
  @Singleton
  def provideActorSystem(config: typeSafe): ActorSystem = ActorSystem("tirro-s3-encryptor-system", config)

  @Provides
  @Singleton
  def provideActorMaterializer(system: ActorSystem): ActorMaterializer = ActorMaterializer()(system)

  @Provides
  @Singleton
  def provideExecutionContext(system: ActorSystem): ExecutionContext = system.dispatcher.asInstanceOf[ExecutionContext]

  @Provides
  @Singleton
  def provideLoggingAdapter(system: ActorSystem): LoggingAdapter = Logging(system, "tirro-s3-encryptor-log")

  @Provides
  @Singleton
  def providesSQSSourceSettings: SqsSourceSettings =
    SqsSourceSettings()

  @Provides
  @Singleton
  def providesSQSAsyncClient: SqsAsyncClient =
    SqsAsyncClient.create()

  @Provides
  @Singleton
  def provideS3Client(log: LoggingAdapter): AmazonS3 = {
    Try(AmazonS3ClientBuilder.defaultClient()) match {
      case Success(s3Client) => s3Client
      case Failure(ex) =>
        log.error(s"Error creating S3 Client ${ex.getMessage}")
        throw new RuntimeException()
    }
  }

  @Provides
  @Singleton
  def provideDecryptor() = new PGPUtils()
}
