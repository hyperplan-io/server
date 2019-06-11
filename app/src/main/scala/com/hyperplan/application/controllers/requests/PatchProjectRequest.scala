package com.foundaml.server.controllers.requests

import com.hyperplan.domain.models.{ProblemType, AlgorithmPolicy}

case class PatchProjectRequest(
    name: Option[String],
    policy: Option[AlgorithmPolicy]
)
