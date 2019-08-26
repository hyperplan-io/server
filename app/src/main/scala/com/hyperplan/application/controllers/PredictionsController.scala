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
import cats.MonadError
import cats.Functor
import io.circe.Json
import io.circe._
import io.circe.parser._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import com.hyperplan.application.AuthenticationMiddleware
import com.hyperplan.application.controllers.requests._
import com.hyperplan.domain.errors._
import com.hyperplan.domain.services._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization.{
  PredictionRequestEntitySerializer,
  PredictionSerializer
}
import com.hyperplan.domain.services.FeaturesParserService
import java.nio.charset.StandardCharsets

import com.hyperplan.domain.errors.PredictionError._
import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.{Prediction, Project, ProjectConfiguration}
import com.hyperplan.infrastructure.serialization.errors.PredictionErrorsSerializer

class PredictionsController(
    projectsService: ProjectsService,
    domainService: DomainService,
    predictionsService: PredictionsService
) extends Http4sDsl[IO]
    with IOLogging {

  val unhandledErrorMessage =
    s"""Unhandled server error, please check the logs or contact support"""

  val parseFeatures: (ProjectConfiguration, Json) => IO[Features] =
    FeaturesParserService.parseFeatures(domainService)

  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          predictionRequest <- req.as[PredictionRequest](
            MonadError[IO, Throwable],
            PredictionRequestEntitySerializer.requestDecoder
          )
          optProject <- projectsService.readProject(predictionRequest.projectId)
          eitherProject <- optProject
            .fold[IO[Either[PredictionError, Prediction]]](
              IO.pure(
                ProjectDoesNotExistError(
                  ProjectDoesNotExistError.message(predictionRequest.projectId)
                ).asLeft
              )
            ) { project =>
              for {
                body <- req.body.compile.toList
                jsonBody <- IO.fromEither(
                  parse(new String(body.toArray, StandardCharsets.UTF_8))
                )
                features <- parseFeatures(
                  project.configuration,
                  jsonBody
                )
                prediction <- predictionsService.predict(
                  predictionRequest.projectId,
                  features,
                  predictionRequest.entityLinks.getOrElse(Nil),
                  predictionRequest.algorithmId
                )
                _ <- logger.debug(
                  s"Prediction computed for project ${predictionRequest.projectId} using algorithm ${predictionRequest.algorithmId}"
                )
              } yield prediction
            }
        } yield eitherProject)
          .flatMap {
            case Right(prediction) =>
              Created(
                PredictionSerializer.encodeJson(prediction)
              )
            case Left(error: BackendExecutionError) =>
              BadGateway(
                PredictionErrorsSerializer.encodeJson(error)
              )
            case Left(error) =>
              BadRequest(
                PredictionErrorsSerializer.encodeJson(error)
              )
          }
          .handleErrorWith { err =>
            logger.warn("Unhandled error in PredictionsController", err) >> InternalServerError(
              unhandledErrorMessage
            )
          }

    }
  }

}
