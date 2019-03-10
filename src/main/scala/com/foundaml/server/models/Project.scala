package com.foundaml.server.models

import java.util.UUID

sealed trait ProblemType

case object Classification extends ProblemType
case object Regression extends ProblemType

sealed trait AlgorithmPolicy[FeatureType, LabelType] {
  def take(): Algorithm[FeatureType, LabelType]
}

case class DefaultAlgorithm[FeatureType, LabelType](algorithm: Algorithm[FeatureType, LabelType]) extends AlgorithmPolicy[FeatureType, LabelType]
case class ABTesting[FeatureType, LabelType](algorithms: List[Algorithm[FeatureType, LabelType]], weights: List[Float]) extends AlgorithmPolicy[FeatureType, LabelType]

case class Project[FeatureType, LabelType](
  id: String,
  name: String,
  problem: ProblemType,
  algorithms: Map[String, Algorithm[FeatureType, LabelType]],
  policy: AlgorithmPolicy[FeatureType, LabelType]
)
