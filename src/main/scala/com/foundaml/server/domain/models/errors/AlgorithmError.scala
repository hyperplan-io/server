package com.foundaml.server.domain.models.errors

sealed trait AlgorithmError extends Throwable {
  override def getMessage: String
}

case class IncompatibleFeatures(message: String) extends AlgorithmError {
  override def getMessage = message
}
case class IncompatibleLabels(message: String) extends AlgorithmError {
  override def getMessage = message
}
case class AlgorithmAlreadyExists(algorithmId: String) extends AlgorithmError {
  override def getMessage = s"The algorithm $algorithmId already exists"
}
case class IncompatibleAlgorithm(algorithmId: String) extends AlgorithmError {
  override def getMessage = s"The algorithm $algorithmId is not compatible"
}
case class AlgorithmDataIncorrect(algorithmId: String) extends AlgorithmError {
  override def getMessage = s"The algorithm $algorithmId data is incorrect"
}
