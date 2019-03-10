package com.foundaml.server.models

import java.util.UUID

case class Project(
    id: String,
    name: String,
    problem: ProblemType,
    featureType: String,
    labelType: String,
    algorithms: Map[String, Algorithm],
    policy: AlgorithmPolicy
)
