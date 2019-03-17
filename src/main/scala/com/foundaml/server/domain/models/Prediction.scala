package com.foundaml.server.domain.models

import com.foundaml.server.domain.models.features.Features
import com.foundaml.server.domain.models.labels.Labels

case class Prediction(
    id: String,
    projectId: String,
    algorithmId: String,
    features: Features,
    labels: Labels,
    examples: Examples
)
