package com.hyperplan.domain.models.labels.transformers

import cats.implicits._

import com.hyperplan.domain.models.labels.RasaNluClassificationLabels
import com.hyperplan.domain.models.LabelVectorDescriptor
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.errors._
import com.hyperplan.domain.services.ExampleUrlService
import com.hyperplan.domain.models._

case class RasaNluLabelsTransformer() {

  def transform(
                 labelsConfiguration: LabelVectorDescriptor,
                 predictionId: String,
                 labels: RasaNluClassificationLabels
  ): Either[RasaNluLabelsTransformerError, Set[ClassificationLabel]] =
    labelsConfiguration.data match {
      case OneOfLabelsDescriptor(oneOf, description) =>
        val classificationLabels = labels.intent_ranking.collect {
          case prediction if oneOf.contains(prediction.name) =>
            ClassificationLabel(
              prediction.name,
              prediction.confidence,
              ExampleUrlService.correctClassificationExampleUrl(
                predictionId,
                prediction.name
              ),
              ExampleUrlService.incorrectClassificationExampleUrl(
                predictionId,
                prediction.name
              )
            )
        }.toSet

        if (classificationLabels.size == oneOf.size) {
          classificationLabels.asRight
        } else {
          RasaNluMissingLabelError().asLeft
        }

      case DynamicLabelsDescriptor(_) =>
        labels.intent_ranking
          .map { prediction =>
            ClassificationLabel(
              prediction.name,
              prediction.confidence,
              ExampleUrlService.correctClassificationExampleUrl(
                predictionId,
                prediction.name
              ),
              ExampleUrlService.incorrectClassificationExampleUrl(
                predictionId,
                prediction.name
              )
            )
          }
          .toSet
          .asRight
    }

}
