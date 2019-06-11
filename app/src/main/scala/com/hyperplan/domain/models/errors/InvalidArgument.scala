package com.hyperplan.domain.models.errors

case class InvalidArgument(message: String) extends Throwable(message)
