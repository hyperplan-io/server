package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models.features._

case class PredictionRequest(
    projectId: String,
    algorithmId: Option[String],
    features: Features
)
