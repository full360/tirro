package com.full360.tirro.s3encryptor.client.integration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.full360.tirro.s3encryptor.config.Config
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ Matchers, WordSpecLike }

trait ClientIntegrationSpec extends WordSpecLike with MockitoSugar with Matchers {

  implicit val system: ActorSystem

  implicit val materializer = ActorMaterializer()

  implicit val context = system.dispatcher

  implicit val logger = system.log

  val config = Config()

}
