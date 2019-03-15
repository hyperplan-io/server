package com.foundaml.server.domain.models

case class Project(
    id: String,
    name: String,
    problem: ProblemType,
    featureType: String,
    labelType: String,
    algorithms: Map[String, Algorithm],
    policy: AlgorithmPolicy
)
