package com.foundaml.server.domain.models

sealed trait FeaturesConfiguration

case class StandardFeaturesConfiguration(
    featuresClass: String,
    featuresSize: Int
) extends FeaturesConfiguration
case class CustomFeaturesConfiguration(featuresClasses: List[String])
    extends FeaturesConfiguration

case class ProjectConfiguration(
    problem: ProblemType,
    features: FeaturesConfiguration,
    labels: Set[String]
)

case class Project(
    id: String,
    name: String,
    configuration: ProjectConfiguration,
    algorithms: List[Algorithm],
    policy: AlgorithmPolicy
) {
  lazy val algorithmsMap: Map[String, Algorithm] =
    algorithms.map(algorithm => algorithm.id -> algorithm).toMap
}
