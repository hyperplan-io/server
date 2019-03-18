package com.foundaml.server.domain.models.errors

case class NotFound(message: String) extends Throwable(message)
