package com.foundaml.server.domain.models.backends

import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer

sealed trait Backend

case class Local(
    computed: Labels
) extends Backend

case class TensorFlowBackend(
    host: String,
    port: String,
    featureTransformer: TensorFlowFeaturesTransformer
) extends Backend
