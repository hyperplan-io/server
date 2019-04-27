package com.foundaml.server.domain.models

import com.foundaml.server.domain.models.backends.Backend

case class Algorithm(
    id: String,
    backend: Backend,
    projectId: String
)
