package com.hyperplan.domain.errors

sealed trait DataStreamError extends Throwable {
  val message: String
  override def getMessage: String = message
}

case class DataStreamTimedOut(message: String) extends DataStreamError
