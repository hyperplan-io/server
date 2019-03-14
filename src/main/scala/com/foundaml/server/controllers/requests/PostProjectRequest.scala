package com.foundaml.server.controllers.requests

import com.foundaml.server.models._

case class PostProjectRequest(
    name: String,
    problem: ProblemType,
    featureType: String,
    labelType: String,
)
