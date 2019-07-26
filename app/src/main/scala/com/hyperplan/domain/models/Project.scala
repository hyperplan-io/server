package com.hyperplan.domain.models

sealed trait ProjectConfiguration {
  val dataStream: Option[StreamConfiguration]
}

case class ClassificationConfiguration(
                                        features: FeatureVectorDescriptor,
                                        labels: LabelVectorDescriptor,
                                        dataStream: Option[StreamConfiguration]
) extends ProjectConfiguration

case class RegressionConfiguration(
                                    features: FeatureVectorDescriptor,
                                    dataStream: Option[StreamConfiguration]
) extends ProjectConfiguration

case class StreamConfiguration(
    topic: String
)

sealed trait Project {
  val id: String
  val name: String
  val problem: ProblemType
  val algorithms: List[Algorithm]
  val policy: AlgorithmPolicy
  val featuresId: String
  val labelsId: Option[String]
  val configuration: ProjectConfiguration

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
  override val problem: ProblemType = Classification
  val featuresId = configuration.features.id
  val labelsId = Some(configuration.labels.id)
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
  override val problem: ProblemType = Regression
  val featuresId = configuration.features.id
  val labelsId = None
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
