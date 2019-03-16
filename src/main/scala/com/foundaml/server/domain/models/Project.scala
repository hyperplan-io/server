package com.foundaml.server.domain.models

case class Project(
    id: String,
    name: String,
    problem: ProblemType,
    featureType: String,
    labelType: String,
    algorithms: List[Algorithm],
    policy: AlgorithmPolicy
) {
  lazy val algorithmsMap: Map[String, Algorithm] =
    algorithms.map(algorithm => algorithm.id -> algorithm).toMap
}
