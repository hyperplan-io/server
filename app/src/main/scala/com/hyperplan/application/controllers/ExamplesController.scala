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

package com.hyperplan.application.controllers

import cats.effect.IO
import cats.implicits._

import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import com.hyperplan.domain.errors.PredictionError
import com.hyperplan.domain.errors.PredictionError._
import com.hyperplan.domain.services.PredictionsService
import com.hyperplan.infrastructure.serialization.events.PredictionEventSerializer
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization.errors.PredictionErrorsSerializer

class ExamplesController(
    predictionsService: PredictionsService
) extends Http4sDsl[IO]
    with IOLogging {

  object ValueIdMatcher extends OptionalQueryParamDecoderMatcher[Float]("value")
  object LabelIdMatcher
      extends OptionalQueryParamDecoderMatcher[String]("label")
  object PredictionIdMatcher
      extends QueryParamDecoderMatcher[String]("predictionId")

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root :? PredictionIdMatcher(predictionId) +& LabelIdMatcher(
            labelId
          ) +& ValueIdMatcher(value) =>
        (for {
          example <- predictionsService.addExample(predictionId, labelId, value)
          _ <- logger.info(s"Example assigned to prediction $predictionId")
        } yield example)
          .flatMap {
            case Right(example) =>
              Created(PredictionEventSerializer.encodeJson(example))
            case Left(error) =>
              BadRequest(
                PredictionErrorsSerializer.encodeJson(error)
              )
          }
          .handleErrorWith { err =>
            logger.error(
              s"Unhandled error in ExamplesController: ${err.getMessage}"
            ) *> InternalServerError(
              unhandledErrorMessage
            )
          }
    }
  }
}
