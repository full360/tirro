package com.full360.tirro.s3encryptor.stream.flow

import java.io.File

import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, GraphDSL }
import com.full360.tirro.s3encryptor.utils.PGPUtils
import com.google.inject.{ Inject, Singleton }
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import com.full360.tirro.s3encryptor.utils.Utils.createTempFile
import com.full360.tirro.s3encryptor.config.Config

@Singleton
class EncryptFileFlow @Inject() (decryptor: PGPUtils, config: Config) {

  def getFlow =
    Flow.fromGraph(getGraph)

  def getGraph =
    GraphDSL.create() { implicit b =>

      val encrypt = b.add(Flow[File]
        .map { file ⇒
          if (config.publicKey.isEmpty) {
            throw new RuntimeException("No key was provided")
          } else {
            Security.addProvider(new BouncyCastleProvider())

            val output = createTempFile(file.getName, ".pgp")
            decryptor.encryptFile(output.getAbsolutePath, file.getAbsolutePath, config.publicKey, false, false)

            output
          }
        })

      FlowShape(encrypt.in, encrypt.out)
    }

}
