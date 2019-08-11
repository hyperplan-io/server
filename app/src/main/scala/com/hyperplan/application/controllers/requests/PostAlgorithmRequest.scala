package com.hyperplan.application.controllers.requests

import com.hyperplan.domain.models.backends.Backend
import com.hyperplan.domain.models.SecurityConfiguration

case class PostAlgorithmRequest(
    backend: Backend,
    security: SecurityConfiguration
)
