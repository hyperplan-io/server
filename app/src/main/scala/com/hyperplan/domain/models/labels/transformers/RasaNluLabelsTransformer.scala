package com.hyperplan.domain.models.labels.transformers

import cats.implicits._

import com.hyperplan.domain.models.labels.RasaNluClassificationLabels
import com.hyperplan.domain.models.LabelsConfiguration
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.models.errors._
import com.hyperplan.domain.services.ExampleUrlService
import com.hyperplan.domain.models._

case class RasaNluLabelsTransformer() {

  def transform(
      labelsConfiguration: LabelsConfiguration,
      predictionId: String,
      labels: RasaNluClassificationLabels
  ): Either[RasaNluLabelsTransformerError, Set[ClassificationLabel]] =
    labelsConfiguration.data match {
      case OneOfLabelsConfiguration(oneOf, description) =>
        val classificationLabels = labels.predictions.collect {
          case prediction if oneOf.contains(prediction.intent) =>
            ClassificationLabel(
              prediction.intent,
              prediction.confidence,
              ExampleUrlService.correctClassificationExampleUrl(
                predictionId,
                prediction.intent
              ),
              ExampleUrlService.incorrectClassificationExampleUrl(
                predictionId,
                prediction.intent
              )
            )
        }.toSet

        if (classificationLabels.size == oneOf.size) {
          classificationLabels.asRight
        } else {
          RasaNluMissingLabelError().asLeft
        }

      case DynamicLabelsConfiguration(_) =>
        labels.predictions
          .map { prediction =>
            ClassificationLabel(
              prediction.intent,
              prediction.confidence,
              ExampleUrlService.correctClassificationExampleUrl(
                predictionId,
                prediction.intent
              ),
              ExampleUrlService.incorrectClassificationExampleUrl(
                predictionId,
                prediction.intent
              )
            )
          }
          .toSet
          .asRight
    }

}
