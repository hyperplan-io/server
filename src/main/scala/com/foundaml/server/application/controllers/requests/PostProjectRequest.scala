package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models._

case class PostProjectRequest(
    name: String,
    problem: ProblemType,
    featureType: String,
    labelType: String
)
