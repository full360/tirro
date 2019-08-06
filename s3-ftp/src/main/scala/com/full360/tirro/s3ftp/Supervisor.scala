package com.full360.tirro.s3ftp

import akka.stream.{ Supervision }
import akka.event.LoggingAdapter

object Supervisor {

  def apply()(implicit log: LoggingAdapter) = {
    getSupervisor()
  }

  def getSupervisor()(implicit log: LoggingAdapter) = {
    val decider: Supervision.Decider = {
      case something ⇒ {
        log.error("Something went wrong" + something + " reason:" + something.getStackTrace.mkString("\n"))
        Supervision.Resume
      }
    }
    decider
  }

}
