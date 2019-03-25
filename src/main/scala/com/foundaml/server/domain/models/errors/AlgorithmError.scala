package com.foundaml.server.domain.models.errors

sealed trait AlgorithmError extends Throwable

case class IncompatibleFeatures(message: String) extends AlgorithmError
case class IncompatibleLabels(message: String) extends AlgorithmError
case class AlgorithmAlreadyExists(algorithmId: String) extends AlgorithmError
case class IncompatibleAlgorithm(algorithmId: String) extends AlgorithmError
case class AlgorithmDataIncorrect(algorithmId: String) extends AlgorithmError
