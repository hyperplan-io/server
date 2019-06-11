package com.hyperplan.domain.models

import com.hyperplan.domain.models.backends.Backend

case class Algorithm(
    id: String,
    backend: Backend,
    projectId: String,
    security: SecurityConfiguration
)
