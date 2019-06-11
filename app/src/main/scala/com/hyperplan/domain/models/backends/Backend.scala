package com.hyperplan.domain.models.backends

import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer

sealed trait Backend

case class LocalClassification(
    computed: Set[ClassificationLabel]
) extends Backend

object LocalClassification {
  val backendClass = "LocalClassification"
}

case class TensorFlowClassificationBackend(
    host: String,
    port: Int,
    featuresTransformer: TensorFlowFeaturesTransformer,
    labelsTransformer: TensorFlowLabelsTransformer
) extends Backend

object TensorFlowClassificationBackend {
  val backendClass = "TensorFlowClassificationBackend"
}

case class TensorFlowRegressionBackend(
    host: String,
    port: Int,
    featuresTransformer: TensorFlowFeaturesTransformer
) extends Backend

object TensorFlowRegressionBackend {
  val backendClass = "TensorFlowRegressionBackend"
}
