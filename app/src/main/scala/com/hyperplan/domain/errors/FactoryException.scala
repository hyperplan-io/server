package com.hyperplan.domain.errors

case class FactoryException(message: String) extends Throwable(message)
