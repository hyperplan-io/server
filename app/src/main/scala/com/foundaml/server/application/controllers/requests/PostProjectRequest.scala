package com.foundaml.server.controllers.requests

import com.foundaml.server.domain.models.ProblemType

case class PostProjectRequest(
  id: String,
  name: String,
  problem: ProblemType,
  featuresId: String,
  labelsId: String
)
