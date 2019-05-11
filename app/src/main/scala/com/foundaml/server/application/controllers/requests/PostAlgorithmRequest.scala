package com.foundaml.server.application.controllers.requests

import com.foundaml.server.domain.models.backends.Backend
import com.foundaml.server.domain.models.SecurityConfiguration

case class PostAlgorithmRequest(
    id: String,
    projectId: String,
    backend: Backend,
    security: SecurityConfiguration 
)
