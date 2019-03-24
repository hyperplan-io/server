package com.foundaml.server.domain.models.labels.transformers

import java.util.UUID

import com.foundaml.server.domain.models.errors.LabelsTransformerError
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.services.ExampleUrlService
import com.foundaml.server.infrastructure.serialization.tensorflow.TensorFlowClassificationLabelsSerializer

case class TensorFlowLabelsTransformer(fields: Map[String, String]) {

  def transform(
      predictionId: String,
      tfLabels: TensorFlowClassificationLabels
  ): Either[LabelsTransformerError, Set[ClassificationLabel]] = {

    val labelsString = tfLabels.result.flatten.map(_._1).toSet

    if (labelsString == fields.keys) {
      val labels: Set[ClassificationLabel] = tfLabels.result.flatten.flatMap {
        case (label, probability) =>
          fields
            .get(label)
            .map { label =>
              val labelId = UUID.randomUUID().toString
              val correctUrl =
                ExampleUrlService.correctClassificationExampleUrl(predictionId, labelId)
              val incorrectUrl =
                ExampleUrlService.incorrectClassificationExampleUrl(predictionId, labelId)
              ClassificationLabel(
                labelId,
                label,
                probability,
                correctUrl,
                incorrectUrl
              )
            }

      }.toSet

      if (labels.size == fields.keys.size) {
        Right(labels)
      } else {
        Left(
          LabelsTransformerError(
            "A label could not be mapped from TensorFlow result"
          )
        )
      }
    } else {
      Left(
        LabelsTransformerError(
          "labels do not exactly match with classification results"
        )
      )
    }
  }
}

object TensorFlowLabelsTransformer {
  def identity(projectLabels: Set[String]) =
    TensorFlowLabelsTransformer(projectLabels.zip(projectLabels).toMap)
}
