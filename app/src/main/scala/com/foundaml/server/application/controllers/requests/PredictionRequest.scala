package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models.features.Features.Features

case class PredictionRequest(
    projectId: String,
    algorithmId: Option[String]
)
