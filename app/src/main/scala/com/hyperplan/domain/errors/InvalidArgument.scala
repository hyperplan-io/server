package com.hyperplan.domain.errors

case class InvalidArgument(message: String) extends Throwable(message)
