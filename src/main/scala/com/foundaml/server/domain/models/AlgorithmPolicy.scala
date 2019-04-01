package com.foundaml.server.domain.models

sealed trait AlgorithmPolicy {
  def take(): Option[String]
  def name: String
}
case class NoAlgorithm() extends AlgorithmPolicy {
  override def take(): Option[String] = None
  val name = NoAlgorithm.name
}

object NoAlgorithm {
  val name = "NoAlgorithm"
}

case class DefaultAlgorithm(algorithmId: String) extends AlgorithmPolicy {
  override def take(): Option[String] = Some(algorithmId)
  val name = DefaultAlgorithm.name
}

object DefaultAlgorithm {
  val name = "DefaultAlgorithm"
}
