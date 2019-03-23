package com.foundaml.server.domain.models.errors

sealed trait AlgorithmError extends Throwable

case class IncompatibleFeatures(message: String) extends AlgorithmError
case class IncompatibleLabels(message: String) extends AlgorithmError
