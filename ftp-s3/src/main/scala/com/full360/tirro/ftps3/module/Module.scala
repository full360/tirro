package com.full360.tirro.ftps3.module

import com.google.inject.{ AbstractModule, Provides, Singleton }
import net.codingwell.scalaguice.ScalaModule
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.{ ExecutionContext, Future }
import akka.NotUsed
import akka.util.ByteString
import com.full360.tirro.ftps3.config.{ Config, ConfigParser }
import akka.stream.alpakka.ftp.scaladsl.Sftp
import akka.stream.alpakka.ftp.FtpFile
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth._
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.alpakka.ftp._
import java.net.InetAddress

import akka.event.LoggingAdapter
import akka.stream.alpakka.s3.scaladsl.S3
import com.amazonaws.regions.{ AwsRegionProvider, DefaultAwsRegionProviderChain }
import com.typesafe.config.{ ConfigFactory, Config => TypeSafeConfig }

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
  def provideConfigLog(args: Config): TypeSafeConfig = ConfigFactory.load(ConfigFactory.parseString(s"""akka{loglevel=${args.loglevel}""").withFallback(ConfigFactory.load))

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
  def provideSFTPSettings(config: Config)(implicit ec: ExecutionContext): SftpSettings = {
    val credentials = FtpCredentials.create(config.ftpUser, config.ftpPass)
    SftpSettings(InetAddress.getByName(config.ftpHost))
      .withCredentials(credentials)
      .withPort(config.ftpPort)
      .withStrictHostKeyChecking(false)
  }

  @Provides
  @Singleton
  def provideSFtpSource(config: Config, settings: SftpSettings): Source[(ByteString, FtpFile), NotUsed] = {
    val sftp = Sftp.ls(config.ftpSourcePath, settings)

    ((config.filter, config.continous) match {
      case (1, 1) =>
        sftp
          .filter(ftpFile => ftpFile.name.startsWith(config.ftpFileFilter) && ftpFile.name.endsWith(config.ftpFileExtFilter))
      case (1, 0) =>
        sftp
          .filter(ftpFile => ftpFile.name.startsWith(config.ftpFileFilter) && ftpFile.name.endsWith(config.ftpFileExtFilter))
          .take(1)
      case (0, 0) =>
        sftp
          .take(1)
      case (_, _) => sftp
    }).flatMapConcat { ftpFile =>
      Sftp.fromPath(ftpFile.path, settings).map({ fileContent =>
        logging.info(s"Downloading file ${ftpFile.name} from: ${ftpFile.path}")
        (fileContent, ftpFile)
      })
    }
  }

  @Provides
  @Singleton
  def provideActorSystem(config: Config): ActorSystem = system

  @Provides
  @Singleton
  def provideActorMaterializer(system: ActorSystem): ActorMaterializer = ActorMaterializer()(system)

  @Provides
  @Singleton
  def provideExecutionContext(system: ActorSystem): ExecutionContext = system.dispatcher.asInstanceOf[ExecutionContext]

  @Provides
  @Singleton
  def provideSink(config: Config)(implicit ec: ExecutionContext): Sink[ByteString, Future[Future[MultipartUploadResult]]] = {
    Sink.lazyInit[ByteString, Future[MultipartUploadResult]](
      _ => {
        logging.info(s"Uploading... ${config.s3BucketKey} to ${config.s3BucketName}")
        Future(S3.multipartUpload(config.s3BucketName, config.s3BucketKey))
      },
      () => Future(MultipartUploadResult("", config.s3BucketName, config.s3BucketKey, "", None))
    )
  }

  @Provides
  @Singleton
  def provideFlow(config: Config): Flow[(ByteString, FtpFile), ByteString, NotUsed] = {
    Flow[(ByteString, FtpFile)].map({ tuple =>
      config.s3BucketKey = config.s3BucketKey + tuple._2.name
      tuple._1
    }).log("middle")

  }
}
