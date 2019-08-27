package com.hyperplan.domain.models.labels.transformers

import cats.implicits._

import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.models.labels.BasicLabel._
import com.hyperplan.domain.models.LabelVectorDescriptor
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.errors._
import com.hyperplan.domain.services.ExampleUrlService
import com.hyperplan.domain.models._

case class BasicLabelsTransformer() {

  def transform(
      labelsConfiguration: LabelVectorDescriptor,
      predictionId: String,
      labels: BasicLabels
  ): Either[BasicLabelsTransformerError, Set[ClassificationLabel]] =
    labelsConfiguration.data match {
      case OneOfLabelsDescriptor(oneOf, description) =>
        val classificationLabels = labels.collect {
          case BasicLabel(label, prob) if oneOf.contains(label) =>
            ClassificationLabel(
              label,
              prob,
              ExampleUrlService.correctClassificationExampleUrl(
                predictionId,
                label
              ),
              ExampleUrlService.incorrectClassificationExampleUrl(
                predictionId,
                label
              )
            )
        }.toSet

        if (classificationLabels.size == oneOf.size) {
          classificationLabels.asRight
        } else {
          BasicLabelMissingError().asLeft
        }

      case DynamicLabelsDescriptor(_) =>
        labels
          .map {
            case BasicLabel(label, prob) =>
              ClassificationLabel(
                label,
                prob,
                ExampleUrlService.correctClassificationExampleUrl(
                  predictionId,
                  label
                ),
                ExampleUrlService.incorrectClassificationExampleUrl(
                  predictionId,
                  label
                )
              )
          }
          .toSet
          .asRight
    }

}
