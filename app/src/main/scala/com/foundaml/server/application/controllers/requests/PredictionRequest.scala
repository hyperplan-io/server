package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.EntityLink

case class PredictionRequest(
    projectId: String,
    algorithmId: Option[String],
    entityLinks: Option[List[EntityLink]]
)
