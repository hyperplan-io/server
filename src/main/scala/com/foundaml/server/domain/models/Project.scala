package com.foundaml.server.domain.models

sealed trait FeaturesConfiguration

case class StandardFeaturesConfiguration(
    featuresClass: String,
    featuresSize: Int,
    description: String
) extends FeaturesConfiguration

case class CustomFeatureConfiguration(key: String, featureClass: String, description: String)

case class CustomFeaturesConfiguration(featuresClasses: List[CustomFeatureConfiguration])
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
