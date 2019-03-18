package com.foundaml.server.domain.models.labels.transformers

import java.util.UUID

import com.foundaml.server.domain.models.errors.LabelsTransformerError
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.services.ExampleUrlService
import com.foundaml.server.infrastructure.serialization.TensorFlowLabelsSerializer

case class TensorFlowLabels(result: List[List[(String, Float)]])

case class TensorFlowLabel(label: String, probability: Float) {
  override def equals(o: Any): Boolean = o match {
    case that: TensorFlowLabel => that.label.equalsIgnoreCase(this.label)
    case _ => false
  }
  override def hashCode: Int = label.toUpperCase.hashCode
}

case class TensorFlowLabelsTransformer(fields: Map[String, String]) {

  def transform(
      predictionId: String,
      tfLabels: TensorFlowLabels
  ): Either[LabelsTransformerError, Labels] = {

    val labelsString = tfLabels.result.flatten.map(_._1).toSet

    if (labelsString == fields.keys) {
      val labels: Set[Label] = tfLabels.result.flatten.flatMap {
        case (label, probability) =>
          fields
            .get(label)
            .map { label =>
              val labelId = UUID.randomUUID().toString
              val correctUrl =
                ExampleUrlService.correctExampleUrl(predictionId, labelId)
              val incorrectUrl =
                ExampleUrlService.incorrectExampleUrl(predictionId, labelId)
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
        Right(Labels(labels))
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
