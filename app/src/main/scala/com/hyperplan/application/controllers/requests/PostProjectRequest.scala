package com.foundaml.server.controllers.requests

import com.hyperplan.domain.models.ProblemType

case class PostProjectRequest(
    id: String,
    name: String,
    problem: ProblemType,
    featuresId: String,
    labelsId: Option[String],
    topic: Option[String]
)
