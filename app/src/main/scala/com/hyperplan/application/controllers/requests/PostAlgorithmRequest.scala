package com.hyperplan.application.controllers.requests

import com.hyperplan.domain.models.backends.Backend
import com.hyperplan.domain.models.SecurityConfiguration

case class PostAlgorithmRequest(
    id: String,
    projectId: String,
    backend: Backend,
    security: SecurityConfiguration
)
