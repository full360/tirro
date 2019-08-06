package com.full360.tirro.s3ftp.stream

import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Flow, GraphDSL, RunnableGraph, Sink, Source }
import akka.util.ByteString
import akka.NotUsed

import scala.concurrent.Future
import com.google.inject.{ Inject, Singleton }
import akka.stream.IOResult
import software.amazon.awssdk.services.sqs.model.Message

@Singleton
class S3toFTPStream @Inject() (source: Source[Message, NotUsed], sink: Sink[ByteString, Future[Future[IOResult]]], flow: Flow[Message, ByteString, NotUsed]) {

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
