package com.foundaml.server.services.infrastructure.http.requests

import com.foundaml.server.models.features._
import com.foundaml.server.models._

case class PostProjectRequest(
    name: String,
    problem: String,
    featureType: String,
    labelType: String,
)
