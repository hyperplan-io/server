package com.foundaml.server.domain.models

sealed trait AlgorithmPolicy {
  def take(): Option[Algorithm]
}
case class NoAlgorithm() extends AlgorithmPolicy {
  override def take(): Option[Algorithm] = None
}

case class DefaultAlgorithm(algorithm: Algorithm) extends AlgorithmPolicy {
  override def take(): Option[Algorithm] = Some(algorithm)
}
