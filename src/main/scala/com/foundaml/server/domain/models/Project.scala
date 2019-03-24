package com.foundaml.server.domain.models

case class FeatureConfiguration(
    name: String,
    featuresType: String,
    description: String
)

case class FeaturesConfiguration(configuration: List[FeatureConfiguration])

sealed trait ProjectConfiguration

case class ClassificationConfiguration(
    features: FeaturesConfiguration,
    labels: Set[String]
) extends ProjectConfiguration

case class RegressionConfiguration(
    features: FeaturesConfiguration
) extends ProjectConfiguration

sealed trait Project {
  def id: String
  def name: String
  def problem: ProblemType
  def algorithms: List[Algorithm]
  def policy: AlgorithmPolicy
  def configuration: ProjectConfiguration

  lazy val algorithmsMap: Map[String, Algorithm] =
    algorithms.map(algorithm => algorithm.id -> algorithm).toMap
}

case class ClassificationProject(
    id: String,
    name: String,
    configuration: ClassificationConfiguration,
    algorithms: List[Algorithm],
    policy: AlgorithmPolicy
) extends Project {
  override def problem: ProblemType = Classification()
}

object ClassificationProject {
  def apply(
      id: String,
      name: String,
      configuration: ClassificationConfiguration,
      algorithms: Option[List[Algorithm]],
      policy: AlgorithmPolicy
  ): ClassificationProject =
    ClassificationProject(id, name, configuration, Nil, policy)

  def apply(
      id: String,
      name: String,
      configuration: ClassificationConfiguration,
      algorithms: Option[List[Algorithm]],
      policy: Option[AlgorithmPolicy]
  ): ClassificationProject =
    ClassificationProject(id, name, configuration, Nil, NoAlgorithm())
}


case class RegressionProject(
    id: String,
    name: String,
    configuration: RegressionConfiguration,
    algorithms: List[Algorithm],
    policy: AlgorithmPolicy
) extends Project {
  override def problem: ProblemType = Regression()
}

object RegressionProject {
  def apply(
      id: String,
      name: String,
      configuration: RegressionConfiguration,
      algorithms: Option[List[Algorithm]],
      policy: AlgorithmPolicy
  ): RegressionProject =
    RegressionProject(id, name, configuration, Nil, policy)

  def apply(
      id: String,
      name: String,
      configuration: RegressionConfiguration,
      algorithms: Option[List[Algorithm]],
      policy: Option[AlgorithmPolicy]
  ): RegressionProject =
    RegressionProject(id, name, configuration, Nil, NoAlgorithm())
}
