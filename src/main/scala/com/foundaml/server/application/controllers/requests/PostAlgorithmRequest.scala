package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models.backends.Backend

case class PostAlgorithmRequest(
    id: String,
    projectId: String,
    backend: Backend
)
