package com.hyperplan.domain.services

import cats.effect.{ContextShift, IO}

import com.hyperplan.domain.errors.PredictionError
import com.hyperplan.domain.models.{Algorithm, Prediction, Project}
import com.hyperplan.domain.models.features.Features

trait BackendService {
  def predictWithBackend(
      predictionId: String,
      project: Project,
      algorithm: Algorithm,
      features: Features.Features
  )(implicit cs: ContextShift[IO]): IO[Either[PredictionError, Prediction]]
}
