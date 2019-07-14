package com.hyperplan.domain.models.backends

import cats.implicits._

import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.models.features.transformers._
import com.hyperplan.domain.models.labels.transformers._
import com.hyperplan.domain.models.errors.LabelsTransformerError

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

case class RasaNluClassificationBackend(
    host: String,
    port: Int,
    featuresTransformer: RasaNluFeaturesTransformer,
    labelsTransformer: RasaNluLabelsTransformer
) extends Backend

object RasaNluClassifcationBackend {
  val backendClass = "RasaNluClassificationBackend"
}

case class TensorFlowRegressionBackend(
    host: String,
    port: Int,
    featuresTransformer: TensorFlowFeaturesTransformer
) extends Backend {
  val labelsTransformer = (tensorFlowLabels: TensorFlowRegressionLabels) => {
    tensorFlowLabels.result.headOption
      .fold[Either[LabelsTransformerError, Set[RegressionLabel]]](
        Left(LabelsTransformerError(""))
      )(
        labels =>
          labels
            .map { label =>
              RegressionLabel(
                label,
                ""
              )
            }
            .toSet
            .asRight
      )
  }
}

object TensorFlowRegressionBackend {
  val backendClass = "TensorFlowRegressionBackend"
}
