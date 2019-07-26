package com.hyperplan.application.controllers.requests

import com.hyperplan.domain.models.{ProblemType, AlgorithmPolicy}

case class PatchProjectRequest(
    name: Option[String],
    policy: Option[AlgorithmPolicy]
)
