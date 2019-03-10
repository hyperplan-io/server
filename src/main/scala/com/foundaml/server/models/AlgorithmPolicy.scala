package com.foundaml.server.models

import java.util.UUID

sealed trait AlgorithmPolicy {
  def take(): Algorithm
}

case class DefaultAlgorithm(algorithm: Algorithm) extends AlgorithmPolicy {
  override def take() = algorithm
}
