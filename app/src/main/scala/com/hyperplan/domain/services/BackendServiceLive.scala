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

import cats.effect.Resource
import cats.effect.IO
import cats.effect.ContextShift
import cats.implicits._
import org.http4s.client.Client
import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  Header,
  Headers,
  MalformedMessageBodyFailure,
  Method,
  Request,
  Uri
}
import java.{util => ju}

import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.errors.PredictionError
import com.hyperplan.domain.errors.PredictionError._
import com.hyperplan.infrastructure.serialization.tensorflow._
import com.hyperplan.infrastructure.serialization.rasa._
import com.hyperplan.infrastructure.logging.IOLogging

import scala.util.Random

class BackendServiceLive(blazeClient: Resource[IO, Client[IO]])
    extends BackendService
    with IOLogging {

  def predictWithBackend(
      predictionId: String,
      project: Project,
      algorithm: Algorithm,
      features: Features.Features
  )(implicit cs: ContextShift[IO]): IO[Either[PredictionError, Prediction]] =
    (algorithm.backend, project) match {
      case (
          LocalRandomClassification(preComputedLabels),
          _: ClassificationProject
          ) =>
        val labelsSize = preComputedLabels.size
        IO.pure(
          ClassificationPrediction(
            predictionId,
            project.id,
            algorithm.id,
            features,
            Nil,
            preComputedLabels.map { label =>
              ClassificationLabel(
                label,
                1f / labelsSize.toFloat,
                ExampleUrlService
                  .correctClassificationExampleUrl(predictionId, label),
                ExampleUrlService.incorrectClassificationExampleUrl(
                  predictionId,
                  label
                )
              )
            }
          ).asRight
        )
      case (
          LocalRandomRegression(),
          _: RegressionProject
          ) =>
        IO.pure(
          RegressionPrediction(
            predictionId,
            project.id,
            algorithm.id,
            features,
            Nil,
            Set(
              RegressionLabel(
                Random.nextFloat(),
                ExampleUrlService.correctRegressionExampleUrl(predictionId)
              )
            )
          ).asRight
        )

      case (
          TensorFlowClassificationBackend(
            rootPath,
            model,
            modelVersion,
            featuresTransformer,
            labelsTransformer
          ),
          classificationProject: ClassificationProject
          ) =>
        val transformedFeatures = featuresTransformer.transform(features)

        val uriString = modelVersion match {
          case Some(value) =>
            s"$rootPath/v1/models/$model/versions/$value:classify"
          case None =>
            s"$rootPath/v1/models/$model:classify"
        }
        buildRequestWithFeatures(
          uriString,
          algorithm.security.headers,
          transformedFeatures
        )(TensorFlowFeaturesSerializer.entityEncoder)
          .fold[IO[Either[PredictionError, Prediction]]](
            err => IO.raiseError(err),
            request =>
              callHttpBackend(
                request,
                labelsTransformer.transform(
                  classificationProject.configuration.labels,
                  predictionId,
                  _: TensorFlowClassificationLabels
                )
              )(TensorFlowClassificationLabelsSerializer.entityDecoder)
                .flatMap {
                  case Right(labels) =>
                    IO.pure(
                      ClassificationPrediction(
                        predictionId,
                        project.id,
                        algorithm.id,
                        features,
                        Nil,
                        labels
                      ).asRight
                    )
                  case Left(err) =>
                    logger.warn(
                      "An error occurred with labels transformer",
                      err
                    ) *> IO.raiseError(err)
                }
                .handleErrorWith {
                  case err =>
                    val error = BackendExecutionError(
                      BackendExecutionError.message(err)
                    ).asLeft
                    logger.warn("An error occurred with backend", err) *> IO
                      .pure(
                        error
                      )
                }
          )

      case (
          RasaNluClassificationBackend(
            rootPath,
            rasaProject,
            rasaModel,
            featuresTransformer,
            labelsTransformer
          ),
          classificationProject: ClassificationProject
          ) =>
        featuresTransformer
          .transform(features, rasaProject, rasaModel)
          .fold[IO[Either[PredictionError, Prediction]]](
            err => IO(FeaturesTransformerError().asLeft),
            transformedFeatures =>
              buildRequestWithFeatures(
                rootPath,
                algorithm.security.headers,
                transformedFeatures
              )(RasaNluFeaturesSerializer.entityEncoder)
                .fold[IO[Either[PredictionError, Prediction]]](
                  err => IO.raiseError(err),
                  request =>
                    callHttpBackend(
                      request,
                      labelsTransformer.transform(
                        classificationProject.configuration.labels,
                        predictionId,
                        _: RasaNluClassificationLabels
                      )
                    )(RasaNluLabelsSerializer.entityDecoder)
                      .flatMap {
                        case Right(labels) =>
                          IO.pure(
                            ClassificationPrediction(
                              predictionId,
                              project.id,
                              algorithm.id,
                              features,
                              Nil,
                              labels
                            ).asRight
                          )
                        case Left(err) =>
                          logger.warn(
                            "An error occurred with labels transformer",
                            err
                          ) *> IO.raiseError(err)
                      }
                      .handleErrorWith { err =>
                        val error = BackendExecutionError(
                          BackendExecutionError.message(err)
                        ).asLeft
                        logger.warn("An error occurred with backend", err) *> IO
                          .pure(
                            error
                          )
                      }
                )
          )

      case (
          backend @ TensorFlowRegressionBackend(
            rootPath,
            model,
            modelVersion,
            featuresTransformer
          ),
          regressionProject: RegressionProject
          ) =>
        val transformedFeatures = featuresTransformer.transform(features)

        val uriString = modelVersion match {
          case Some(value) =>
            s"$rootPath/v1/models/$model/versions/$value:regress"
          case None =>
            s"$rootPath/v1/models/$model:regress"
        }
        buildRequestWithFeatures(
          uriString,
          algorithm.security.headers,
          transformedFeatures
        )(TensorFlowFeaturesSerializer.entityEncoder)
          .fold[IO[Either[PredictionError, Prediction]]](
            err => IO.raiseError(err),
            request =>
              callHttpBackend(
                request,
                backend.labelsTransformer(
                  _: TensorFlowRegressionLabels,
                  predictionId
                )
              )(TensorFlowRegressionLabelsSerializer.entityDecoder)
                .flatMap {
                  case Right(labels) =>
                    IO.pure(
                      RegressionPrediction(
                        predictionId,
                        project.id,
                        algorithm.id,
                        features,
                        Nil,
                        labels
                      ).asRight
                    )
                  case Left(err) =>
                    logger.warn(
                      "An error occurred with labels transformer",
                      err
                    ) *> IO.raiseError(err)
                }
                .handleErrorWith { err =>
                  val error = BackendExecutionError(
                    BackendExecutionError.message(err)
                  ).asLeft
                  logger.warn("An error occurred with backend", err) *> IO.pure(
                    error
                  )
                }
          )

      case (backend, _) =>
        val errorMessage =
          s"Backend ${backend.getClass.getSimpleName} is not compatible with project ${project.getClass.getSimpleName}"
        logger.warn(errorMessage) >> IO(
          IncompatibleBackendError(
            IncompatibleBackendError.message(
              backend,
              project
            )
          ).asLeft
        )

    }

  def buildRequestWithFeatures[F, L](
      uriString: String,
      headers: List[(String, String)],
      features: F
  )(
      implicit entityEncoder: EntityEncoder[IO, F]
  ): Either[Throwable, Request[IO]] =
    Uri
      .fromString(uriString)
      .fold(
        err => Left(err),
        uri =>
          Right(
            Request[IO](method = Method.POST, uri = uri)
              .withHeaders(Headers(headers.map {
                case (key, value) => Header(key, value)
              }))
              .withEntity(features)
          )
      )

  def callHttpBackend[L, T, E](
      request: Request[IO],
      labelToPrediction: L => Either[E, Set[T]]
  )(
      implicit entityDecoder: EntityDecoder[IO, L]
  ): IO[Either[E, Set[T]]] =
    blazeClient.use(
      _.expect[L](request)(entityDecoder).map(labelToPrediction)
    )

}
