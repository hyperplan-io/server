package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models._

case class PostProjectConfiguration(
    problem: ProblemType,
    features: FeaturesConfiguration,
    labels: Set[String]
)

case class PostProjectRequest(
    id: String,
    name: String,
    configuration: PostProjectConfiguration
)
