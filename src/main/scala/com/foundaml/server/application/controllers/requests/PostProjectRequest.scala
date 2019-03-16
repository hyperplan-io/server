package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models._

case class PostProjectConfiguration(
    problem: ProblemType,
    featuresClass: String,
    featuresSize: Int,
    labels: Set[String]
)

case class PostProjectRequest(
    name: String
    ,configuration: PostProjectConfiguration
)
