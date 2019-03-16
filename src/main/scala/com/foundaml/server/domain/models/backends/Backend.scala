package com.foundaml.server.domain.models.backends

import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabelsTransformer

sealed trait Backend

case class Local(
    computed: Labels
) extends Backend

case class TensorFlowBackend(
    host: String,
    port: Int,
    featuresTransformer: TensorFlowFeaturesTransformer,
    labelsTransformer: TensorFlowLabelsTransformer
) extends Backend
