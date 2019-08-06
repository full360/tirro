package com.full360.tirro.s3ftp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success, Try }
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import com.google.inject.{ Guice, Injector }
import com.full360.tirro.s3ftp.stream.S3toFTPStream
import com.full360.tirro.s3ftp.module.Module
import akka.stream.ActorAttributes
import software.amazon.awssdk.services.sqs.SqsAsyncClient

object S3Ftp {
  implicit val system = ActorSystem("Tirro-S3-FTP")
  implicit val materializer = ActorMaterializer()
  implicit val logging = Logging(system, "tirro-s3-ftp")

  val supervisor = Supervisor()

  def main(args: Array[String]): Unit = {
    try {
      start(Guice.createInjector(new Module(args, system, materializer, logging))) match {
        case Success(_)     ⇒
        case Failure(error) ⇒ logging.error(error, "Error raised while creating dependencies"); exit()
      }
    } catch {
      case e: Exception ⇒
        logging.error("something went wrong:" + e.getMessage)
        logging.error("shutting down")
        exit();
    }
  }

  def start(injector: Injector) = Try {

    injector
      .getInstance(classOf[S3toFTPStream])
      .getStream
      .withAttributes(ActorAttributes.supervisionStrategy(supervisor))
      .run()
      .flatten
      .onComplete { result ⇒
        result match {
          case Success(byte)  ⇒ logging.info(s"Success: ${byte.toString}");
          case Failure(error) ⇒ logging.error(s"Error raised on stream $error")
        }

        sys.addShutdownHook({
          injector.getInstance(classOf[SqsAsyncClient]).close()
        })
        exit()
      }
  }

  def exit() = {
    materializer.shutdown()
    system.terminate().onComplete {
      case Success(_) ⇒
        println("Terminated")
        sys.exit(0)
      case Failure(error) ⇒
        println(s"Terminated with error $error")
        sys.exit(1)
    }
  }
}
