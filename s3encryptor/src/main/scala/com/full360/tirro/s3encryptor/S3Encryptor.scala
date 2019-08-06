package com.full360.tirro.s3encryptor

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.full360.tirro.s3encryptor.module.Module
import com.full360.tirro.s3encryptor.stream.S3EncryptorStream
import com.google.inject.Guice
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

object S3Encryptor {

  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new Module(args))
    sys.addShutdownHook({
      injector.getInstance(classOf[ActorMaterializer]).shutdown()
      injector.getInstance(classOf[ActorSystem]).terminate()
      injector.getInstance(classOf[SqsAsyncClient]).close()
    })

    implicit val materializer = injector.getInstance(classOf[ActorMaterializer])

    implicit val system = injector.getInstance(classOf[ActorSystem])

    implicit val ec: ExecutionContext = system.dispatcher.asInstanceOf[ExecutionContext]

    try {
      injector.getInstance(classOf[S3EncryptorStream]).getStream
        .run()
        .onComplete { result ⇒
          result match {
            case Success(byte)  ⇒ println(s"Success: ${byte.toString}");
            case Failure(error) ⇒ Console.err.println(s"Error ${error.getMessage}");
          }

          materializer.shutdown()
          system.terminate().onComplete {
            case Success(_) ⇒ println("Terminated")
            case Failure(e) ⇒ println(s"Terminated with error + ${e.getMessage}")
          }
          sys.exit()
        }
    } catch {
      case e: Exception ⇒
        Console.err.println("something went wrong:" + e.getMessage)
        Console.err.println("shutting down")
        materializer.shutdown()
        system.terminate()
        sys.exit()
    }
  }
}
