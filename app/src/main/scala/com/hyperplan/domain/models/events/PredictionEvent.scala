package com.hyperplan.domain.models.events

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.labels.{ClassificationLabel, RegressionLabel}

sealed trait PredictionEvent {
  def eventType: String
  def projectId: String
}

case class ClassificationPredictionEvent(
    id: String,
    predictionId: String,
    projectId: String,
    algorithmId: String,
    features: Features,
    labels: Set[ClassificationLabel],
    example: String
) extends PredictionEvent {
  override val eventType: String = ClassificationPredictionEvent.eventType
}

object ClassificationPredictionEvent {
  val eventType = "classification"
}

case class RegressionPredictionEvent(
    id: String,
    predictionId: String,
    projectId: String,
    algorithmId: String,
    features: Features,
    labels: Set[RegressionLabel],
    example: Float
) extends PredictionEvent {
  override val eventType: String = RegressionPredictionEvent.eventType
}
object RegressionPredictionEvent {
  val eventType = "regression"
}
