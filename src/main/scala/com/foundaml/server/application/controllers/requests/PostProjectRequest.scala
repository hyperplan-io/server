package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models._

case class PostProjectConfiguration(
    problem: ProblemType,
    featureClass: String,
    featuresSize: Int,
    labelsClass: String,
    labelsSize: Int
)

case class PostProjectRequest(
    name: String,
    configuration: PostProjectConfiguration
)
