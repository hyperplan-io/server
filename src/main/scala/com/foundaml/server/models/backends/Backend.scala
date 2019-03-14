package com.foundaml.server.models.backends

import com.foundaml.server.models.labels._
import com.foundaml.server.models.features.transformers.TensorFlowFeaturesTransformer

sealed trait Backend

case class Local(
    computed: Labels
) extends Backend

case class TensorFlowBackend(host: String, port: String, featureTransformer: TensorFlowFeaturesTransformer) extends Backend

