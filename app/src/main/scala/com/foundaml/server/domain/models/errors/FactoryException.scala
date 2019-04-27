package com.foundaml.server.domain.models.errors

case class FactoryException(message: String) extends Throwable(message)
