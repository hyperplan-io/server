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

package com.hyperplan.application.controllers.grpc

import cats.effect.IO
import cats.implicits._
import com.hyperplan.domain.errors.PredictionError.ProjectDoesNotExistError
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models._
import com.hyperplan.domain.services.{
  DomainService,
  PredictionsService,
  ProjectsService
}
import com.hyperplan.protos.prediction.Label.Value.ClassificationLabel
import com.hyperplan.protos.prediction.{
  PredictionFs2Grpc,
  PredictionRequest,
  PredictionResponse
}
import io.grpc.Metadata

import scala.concurrent.Future

class PredictionGrpcController(
    projectsService: ProjectsService,
    domainService: DomainService,
    predictionsService: PredictionsService
) extends PredictionFs2Grpc[IO, Metadata] {

  private def requestToFeature(
      requestFeatures: Map[String, com.hyperplan.protos.prediction.Feature],
      featureVectorDescriptor: FeatureVectorDescriptor
  ) =
    featureVectorDescriptor.data.map { descriptor =>
      val featureName = descriptor.name
      (descriptor.featuresType, descriptor.dimension) match {
        case (FloatFeatureType, Scalar) =>
          requestFeatures.get(featureName).map(_.getFloatScalar).map { f =>
            FloatFeature(featureName, f)
          }
        case (FloatFeatureType, Array) =>
          requestFeatures.get(featureName).map(_.getFloatArray).map { f =>
            FloatArrayFeature(featureName, f.value.toList)
          }
        case (FloatFeatureType, Matrix) =>
          requestFeatures.get(featureName).map(_.getFloatMatrix).map { f =>
            FloatMatrixFeature(featureName, f.value.map(_.value.toList).toList)
          }
        case (IntFeatureType, Scalar) =>
          requestFeatures.get(featureName).map(_.getIntScalar).map { f =>
            IntFeature(featureName, f)
          }
        case (IntFeatureType, Array) =>
          requestFeatures.get(featureName).map(_.getIntArray).map { f =>
            IntArrayFeature(featureName, f.value.toList)
          }
        case (IntFeatureType, Matrix) =>
          requestFeatures.get(featureName).map(_.getIntMatrix).map { f =>
            IntMatrixFeature(featureName, f.value.map(_.value.toList).toList)
          }
        case (StringFeatureType, Scalar) =>
          requestFeatures.get(featureName).map(_.getStringScalar).map { f =>
            StringFeature(featureName, f)
          }
        case (StringFeatureType, Array) =>
          requestFeatures.get(featureName).map(_.getStringArray).map { f =>
            StringArrayFeature(featureName, f.value.toList)
          }
        case (StringFeatureType, Matrix) =>
          requestFeatures.get(featureName).map(_.getStringMatrix).map { f =>
            StringMatrixFeature(featureName, f.value.map(_.value.toList).toList)
          }
        case (_: ReferenceFeatureType, _) =>
          none[Feature]
      }
    }

  override def predict(
      request: PredictionRequest,
      ctx: Metadata
  ): IO[PredictionResponse] = {
    val algorithmId =
      if (request.algorithmId.isEmpty) None else request.algorithmId.some
    val projectId = request.projectId
    projectsService.readProject(projectId).flatMap {
      case Some(project) =>
        val requestFeatures = request.features
        val maybeFeatures = project.configuration match {
          case ClassificationConfiguration(features, labels, dataStream) =>
            requestToFeature(requestFeatures, features)
          case RegressionConfiguration(features, dataStream) =>
            requestToFeature(requestFeatures, features)
        }

        if (maybeFeatures.flatten.length != maybeFeatures.length) {
          IO.raiseError(
            new Exception(
              "Missing features in Grpc data"
            )
          )
        } else {
          predictionsService
            .predict(
              request.projectId,
              maybeFeatures.flatten,
              Nil,
              algorithmId
            )
            .flatMap {
              case Right(prediction) =>
                val (labels, examples) = prediction match {
                  case ClassificationPrediction(
                      id,
                      projectId,
                      algorithmId,
                      features,
                      examples,
                      labels
                      ) =>
                    labels.map { label =>
                      com.hyperplan.protos.prediction
                        .Label()
                        .withClassificationLabel(
                          com.hyperplan.protos.prediction.ClassificationLabel(
                            label.label,
                            label.probability,
                            label.correctExampleUrl,
                            label.incorrectExampleUrl
                          )
                        )
                    }.toSeq -> examples.map { example =>
                      com.hyperplan.protos.prediction
                        .Example()
                        .withClassificationExample(
                          com.hyperplan.protos.prediction.ClassificationExample(
                            example
                          )
                        )
                    }

                  case RegressionPrediction(
                      id,
                      projectId,
                      algorithmId,
                      features,
                      examples,
                      labels
                      ) =>
                    labels.map { label =>
                      com.hyperplan.protos.prediction
                        .Label()
                        .withRegressionLabel(
                          com.hyperplan.protos.prediction.RegressionLabel(
                            label.label,
                            label.correctExampleUrl
                          )
                        )
                    }.toSeq -> examples.map { example =>
                      com.hyperplan.protos.prediction
                        .Example()
                        .withRegressionExample(
                          com.hyperplan.protos.prediction.RegressionExample(
                            example
                          )
                        )
                    }

                }
                IO.pure(
                  PredictionResponse(
                    prediction.id,
                    prediction.projectId,
                    prediction.algorithmId,
                    request.features.values.toSeq,
                    labels,
                    examples
                  )
                )
              case Left(error) =>
                IO.raiseError(
                  error
                )
            }
        }

      case None =>
        IO.raiseError(
          ProjectDoesNotExistError(
            ProjectDoesNotExistError.message(projectId)
          )
        )
    }
  }
}
