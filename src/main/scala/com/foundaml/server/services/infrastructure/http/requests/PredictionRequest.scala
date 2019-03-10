package com.foundaml.server.services.infrastructure.http.requests

import com.foundaml.server.models.features._

case class PredictionRequest(
    projectId: String,
    algorithmId: Option[String],
    features: Features
)
