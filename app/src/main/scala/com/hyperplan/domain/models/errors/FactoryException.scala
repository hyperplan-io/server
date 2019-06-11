package com.hyperplan.domain.models.errors

case class FactoryException(message: String) extends Throwable(message)
