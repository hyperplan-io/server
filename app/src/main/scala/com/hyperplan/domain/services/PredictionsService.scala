/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

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
