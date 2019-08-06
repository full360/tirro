package com.full360.tirro.ftps3.stream

import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Flow, GraphDSL, RunnableGraph, Sink, Source }
import akka.util.ByteString
import akka.stream.alpakka.ftp.FtpFile
import akka.NotUsed
import akka.stream.alpakka.s3.MultipartUploadResult

import scala.concurrent.Future
import com.google.inject.{ Inject, Singleton }

@Singleton
class FTPtoS3Stream @Inject() (source: Source[(ByteString, FtpFile), NotUsed], sink: Sink[ByteString, Future[Future[MultipartUploadResult]]], flow: Flow[(ByteString, FtpFile), ByteString, NotUsed]) {

  def getStream = RunnableGraph
    .fromGraph(getGraph)

  def getGraph = GraphDSL
    .create(sink) { implicit b ⇒ sk ⇒
      val s = b.add(source)
      val f = b.add(flow)

      s.out ~> f ~> sk.in

      ClosedShape
    }
}
