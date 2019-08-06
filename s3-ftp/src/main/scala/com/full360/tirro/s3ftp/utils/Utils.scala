package com.full360.tirro.s3ftp

import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.util.{ Failure, Success, Try }
import com.full360.tirro.s3ftp.domain._

object Utils {

  implicit val formats = org.json4s.DefaultFormats

  def parseToRecord(event: String): Option[List[Record]] = {
    Try((parse(event) \ "Records").extract[List[Record]]) match {
      case Success(x) if x.nonEmpty =>
        Some(x)
      case Success(_) =>
        None
      case Failure(e) =>
        println(e.getMessage)
        None
    }
  }

  def removeS3OriginalPath(s3FileKey: String): String =
    s3FileKey.split("/").last
}