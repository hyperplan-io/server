package com.foundaml.server.models

sealed trait AlgorithmPolicy {
  def take(): Option[Algorithm]
}
case class NoAlgorithm() extends AlgorithmPolicy {
  override def take() = None
}

case class DefaultAlgorithm(algorithm: Algorithm) extends AlgorithmPolicy {
  override def take() = Some(algorithm)
}
