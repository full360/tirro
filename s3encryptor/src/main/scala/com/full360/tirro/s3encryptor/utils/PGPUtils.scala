package com.full360.tirro.s3encryptor.utils

import java.io.{ BufferedOutputStream, ByteArrayOutputStream, ByteArrayInputStream, File, FileInputStream, FileOutputStream, InputStream, OutputStream }
import java.security.SecureRandom
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.openpgp.{ PGPPublicKeyRingCollection, PGPCompressedData, PGPEncryptedDataGenerator, PGPEncryptedDataList, PGPException, PGPLiteralData, PGPPrivateKey, PGPPublicKey, PGPPublicKeyEncryptedData, PGPSecretKeyRingCollection, PGPUtil, PGPCompressedDataGenerator }
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.{ JcePBESecretKeyDecryptorBuilder, JcaKeyFingerprintCalculator, JcePGPDataEncryptorBuilder, JcePublicKeyDataDecryptorFactoryBuilder, JcePublicKeyKeyEncryptionMethodGenerator }
import org.bouncycastle.util.io.Streams

class PGPUtils {

  def decryptFile(
    input:           String,
    key:             String,
    passwd:          String,
    defaultFileName: String
  ): ByteArrayInputStream =
    {
      val fileStream = new FileInputStream(input)
      val in = fileStream
      val keyIn = new ByteArrayInputStream(java.util.Base64.getDecoder.decode(key))
      val out = decryptFile(in, keyIn, passwd, defaultFileName)
      keyIn.close()
      in.close()
      out
    }

  def decryptFile(
    input:           InputStream,
    key:             String,
    passwd:          String,
    defaultFileName: String      = ""
  ): ByteArrayInputStream =
    {
      val in = input
      val keyIn = new ByteArrayInputStream(java.util.Base64.getDecoder.decode(key))
      val out = decryptFile(in, keyIn, passwd, defaultFileName)
      keyIn.close()
      in.close()
      out
    }

  /**
   * decrypt the passed in message stream
   */
  def decryptFile(
    in:              InputStream,
    keyIn:           InputStream,
    passwd:          String,
    defaultFileName: String
  ): ByteArrayInputStream =
    {
      val inS = PGPUtil.getDecoderStream(in)
      val pgpF = new JcaPGPObjectFactory(inS)

      val o = pgpF.nextObject()
      //
      // the first object might be a PGP marker packet.
      //

      val enc = o match {
        case o: PGPEncryptedDataList ⇒ o
        case _ ⇒ pgpF.nextObject() match {
          case nO: PGPEncryptedDataList ⇒ nO
          case _                        ⇒ throw new PGPException("encrypted message contains a signed message - not literal data.");
        }
      }

      //
      // find the secret key
      //
      val it = enc.getEncryptedDataObjects
      var sKey: PGPPrivateKey = null
      var pbe: PGPPublicKeyEncryptedData = null
      val pgpSec = new PGPSecretKeyRingCollection(
        PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator()
      )

      while (sKey == null && it.hasNext) {
        pbe = it.next().asInstanceOf[PGPPublicKeyEncryptedData]
        sKey = findSecretKey(pgpSec, pbe.getKeyID, passwd)
      }

      if (sKey == null) {
        throw new IllegalArgumentException("secret key for message not found.")
      }

      val clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(sKey))
      val plainFact = new JcaPGPObjectFactory(clear)
      var message = plainFact.nextObject()

      message = message match {
        case m1: PGPCompressedData ⇒
          val cData = m1
          val pgpFact = new JcaPGPObjectFactory(cData.getDataStream)
          pgpFact.nextObject()
        case _ ⇒ message
      }

      val bytes = message match {
        case m3: PGPLiteralData ⇒
          val ld = m3
          val is = ld.getInputStream
          val bytesOut = new ByteArrayOutputStream()
          val fOut = new BufferedOutputStream(bytesOut)
          Streams.pipeAll(is, fOut)
          fOut.close()
          bytesOut.toByteArray
        case _ ⇒
          println("message is not a simple encrypted file - type unknown.")
          new Array[Byte](0)
      }

      new ByteArrayInputStream(bytes)
    }

  def encryptFile(
    outputFileName:     String,
    inputFileName:      String,
    encKeyFileName:     String,
    armor:              Boolean = false,
    withIntegrityCheck: Boolean = false
  ): Unit =
    {
      val out = new BufferedOutputStream(new FileOutputStream(outputFileName))
      val encKey = readPublicKey(encKeyFileName)
      encryptFile(out, inputFileName, encKey, armor, withIntegrityCheck)
      out.close()
    }

  def encryptFile(
    out:                OutputStream,
    fileName:           String,
    encKey:             PGPPublicKey,
    armor:              Boolean,
    withIntegrityCheck: Boolean
  ): Unit = {

    val bytes = compressFile(fileName, CompressionAlgorithmTags.ZIP)

    val encGen = new PGPEncryptedDataGenerator(
      new JcePGPDataEncryptorBuilder(7).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC")
    )

    encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("BC"))

    val cOut = encGen.open(out, bytes.length)

    cOut.write(bytes)
    cOut.close()

  }

/***************************************/
  def compressFile(fileName: String, algorithm: Int) =
    {
      val bOut = new ByteArrayOutputStream()
      val comData = new PGPCompressedDataGenerator(algorithm)
      PGPUtil.writeFileToLiteralData(comData.open(bOut), PGPLiteralData.BINARY,
        new File(fileName))
      comData.close()
      bOut.toByteArray
    }

  /**
   * Search a secret key ring collection for a secret key corresponding to keyID if it
   * exists.
   *
   * @param pgpSec a secret key ring collection.
   * @param keyID keyID we want.
   * @param pass passphrase to decrypt secret key with.
   * @return the private key.
   * @throws PGPException
   */
  def findSecretKey(pgpSec: PGPSecretKeyRingCollection, keyID: Long, pass: String): PGPPrivateKey =
    {
      val pgpSecKey = pgpSec.getSecretKey(keyID);

      if (pgpSecKey == null) {
        return null
      }

      pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass.toArray))
    }

  def readPublicKey(key: String): PGPPublicKey =
    {
      val keyIn = new ByteArrayInputStream(java.util.Base64.getDecoder.decode(key))
      val pubKey = readPublicKey(keyIn)
      keyIn.close()
      pubKey
    }

  /**
   * A simple routine that opens a key ring file and loads the first available key
   * suitable for encryption.
   *
   * @param input data stream containing the public key data
   * @return the first public key found.
   * @throws PGPException
   */
  def readPublicKey(input: InputStream): PGPPublicKey =
    {
      var key: PGPPublicKey = null
      val pgpPub = new PGPPublicKeyRingCollection(
        PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator()
      )

      //
      // we just loop through the collection till we find a key suitable for encryption, in the real
      // world you would probably want to be a bit smarter about this.
      //

      val keyRingIter = pgpPub.getKeyRings()
      while (keyRingIter.hasNext) {
        val keyRing = keyRingIter.next()

        val keyIter = keyRing.getPublicKeys
        while (keyIter.hasNext) {
          key = keyIter.next()

          if (key.isEncryptionKey) {
            key
          }
        }
      }

      if (key == null) {
        throw new IllegalArgumentException(
          "Can't find signing key in key ring."
        )
      }
      key
    }
}
