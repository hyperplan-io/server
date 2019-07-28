package com.hyperplan.domain.models.backends

import cats.implicits._

import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.models.features.transformers._
import com.hyperplan.domain.models.labels.transformers._
import com.hyperplan.domain.errors._
import com.hyperplan.domain.services.ExampleUrlService

/**
  * A Backend is what is going to execute the inference task. It can be local or remote.
  */
sealed trait Backend

/**
  * Describe a computation that is already available. Useful for testing purpose.
  * @param computed The labels that are returned.
  */
case class LocalClassification(
    computed: Set[ClassificationLabel]
) extends Backend

object LocalClassification {
  val backendClass = "LocalClassification"
}

/**
  * TensorFlow serving Backend for classification over http protocol
  * @param host the host where TensorFlow serving is served
  * @param port the port on which TensorFlow serving is running
  * @param featuresTransformer the features transformation
  * @param labelsTransformer the labels transformation
  */
case class TensorFlowClassificationBackend(
    host: String,
    port: Int,
    featuresTransformer: TensorFlowFeaturesTransformer,
    labelsTransformer: TensorFlowLabelsTransformer
) extends Backend

object TensorFlowClassificationBackend {
  val backendClass = "TensorFlowClassificationBackend"
}

/**
  * Rasa NLU serving Backend for classification over http protocol
  * @param rootPath the path on which Rasa NLU is served
  * @param project the project of the algorithm
  * @param model the model that will be executed
  * @param featuresTransformer the features transformation
  * @param labelsTransformer the labels transformation
  */
case class RasaNluClassificationBackend(
    rootPath: String,
    project: String,
    model: String,
    featuresTransformer: RasaNluFeaturesTransformer,
    labelsTransformer: RasaNluLabelsTransformer
) extends Backend

object RasaNluClassificationBackend {
  val backendClass = "RasaNluClassificationBackend"
}

/**
  * TensorFlow serving Backend for regression over http protocol
  * @param host the host where TensorFlow serving is served
  * @param port the port on which TensorFlow serving is running
  * @param featuresTransformer the features transformation
  * @param labelsTransformer the labels transformation
  */
case class TensorFlowRegressionBackend(
    host: String,
    port: Int,
    featuresTransformer: TensorFlowFeaturesTransformer
) extends Backend {
  val labelsTransformer =
    (tensorFlowLabels: TensorFlowRegressionLabels, predictionId: String) => {
      tensorFlowLabels.result.headOption
        .fold[Either[LabelsTransformerError, Set[RegressionLabel]]](
          Left(LabelsTransformerError(""))
        )(
          labels =>
            labels
              .map { label =>
                RegressionLabel(
                  label,
                  ExampleUrlService.correctRegressionExampleUrl(predictionId)
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
