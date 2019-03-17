package com.foundaml.server.domain.models

case class ProjectConfiguration(
    problem: ProblemType,
    featureClass: String,
    featuresSize: Int,
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
