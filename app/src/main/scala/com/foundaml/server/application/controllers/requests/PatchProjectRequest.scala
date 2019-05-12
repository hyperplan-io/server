package com.foundaml.server.controllers.requests

import com.foundaml.server.domain.models.{ProblemType, AlgorithmPolicy}

case class PatchProjectRequest(
    name: Option[String],
    policy: Option[AlgorithmPolicy]
)
