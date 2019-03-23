package com.foundaml.server.domain.models

case class FeatureConfiguration(
    name: String,
    featuresType: String,
    description: String
)

case class FeaturesConfiguration(configuration: List[FeatureConfiguration])

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
