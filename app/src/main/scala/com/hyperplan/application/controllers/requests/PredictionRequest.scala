package com.hyperplan.application.controllers.requests

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.EntityLink

case class PredictionRequest(
    projectId: String,
    algorithmId: Option[String],
    entityLinks: Option[List[EntityLink]]
)
