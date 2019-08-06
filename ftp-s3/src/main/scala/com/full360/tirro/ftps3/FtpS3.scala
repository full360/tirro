package com.full360.tirro.ftps3

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success, Try }
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import com.google.inject.{ Guice, Injector }
import com.full360.tirro.ftps3.stream.FTPtoS3Stream
import com.full360.tirro.ftps3.module.Module

object FtpS3 {
  implicit val system = ActorSystem("Tirro-FTP-S3")
  implicit val materializer = ActorMaterializer()
  implicit val log = Logging(system, "tirro-ftp-s3")

  def main(args: Array[String]): Unit = {
    try {
      start(Guice.createInjector(new Module(args, system, materializer, log))) match {
        case Success(_)     ⇒
        case Failure(error) ⇒ log.error(error, "Error raised while creating dependencies"); exit()
      }
    } catch {
      case e: Exception ⇒
        log.error("something went wrong:" + e.getMessage)
        log.error("shutting down")
        exit();
    }
  }

  def start(injector: Injector) = Try(
    injector
      .getInstance(classOf[FTPtoS3Stream])
      .getStream
      .run()
      .flatten
      .onComplete { result ⇒
        result match {
          case Success(byte) ⇒
            log.info(s"Uploaded: ${byte.etag.length}")
            log.info(s"Success: ${byte.toString}");
          case Failure(error) ⇒
            log.error(error, "Error raised on stream")
        }
        exit()
      }
  )

  def exit() = {
    system.terminate()
    materializer.shutdown()
    system.terminate().onComplete {
      case Success(_)     ⇒ log.info("Terminated")
      case Failure(error) ⇒ log.error(error, "Terminated with error")
    }
    sys.exit()
  }
}
