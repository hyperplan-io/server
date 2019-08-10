package com.hyperplan.domain.services

import cats.effect.IO

import com.hyperplan.domain.errors.PredictionError
import com.hyperplan.domain.models.{EntityLink, Prediction, StreamConfiguration}
import com.hyperplan.domain.models.events.PredictionEvent
import com.hyperplan.domain.models.features.Features.Features

trait PredictionsService {
  def persistPrediction(
      prediction: Prediction,
      entityLinks: List[EntityLink]
  ): IO[Either[PredictionError, Int]]
  def publishToStream(
      prediction: PredictionEvent,
      streamConfiguration: Option[StreamConfiguration]
  ): IO[Unit]
  def predict(
      projectId: String,
      features: Features,
      entityLinks: List[EntityLink],
      optionalAlgorithmId: Option[String]
  ): IO[Either[PredictionError, Prediction]]
  def addExample(
      predictionId: String,
      labelOpt: Option[String],
      valueOpt: Option[Float]
  ): IO[Either[PredictionError, PredictionEvent]]
}
