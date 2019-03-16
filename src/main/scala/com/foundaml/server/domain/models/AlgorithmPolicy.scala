package com.foundaml.server.domain.models

sealed trait AlgorithmPolicy {
  def take(): Option[String]
}
case class NoAlgorithm() extends AlgorithmPolicy {
  override def take(): Option[String] = None
}

case class DefaultAlgorithm(algorithmId: String) extends AlgorithmPolicy {
  override def take(): Option[String] = Some(algorithmId)
}
