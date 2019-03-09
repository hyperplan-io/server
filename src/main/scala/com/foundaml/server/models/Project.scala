package com.foundaml.server.models

import java.util.UUID

sealed trait ProblemType

case object Classification extends ProblemType
case object Regression extends ProblemType

sealed trait AlgorithmPolicy {
  def take(): Algorithm
}

case class DefaultAlgorithm(algorithm: Algorithm) extends AlgorithmPolicy {
  override def take() = algorithm
}
//case class ABTesting[FeatureType, LabelType](algorithms: List[Algorithm[FeatureType, LabelType]], weights: List[Float]) extends AlgorithmPolicy[FeatureType, LabelType]

case class Project(
  id: String,
  name: String,
  problem: ProblemType,
  featureType: String,
  labelType: String,
  algorithms: Map[String, Algorithm],
  policy: AlgorithmPolicy
)
