package com.hyperplan.domain.services

import cats.effect.Resource
import org.http4s.client.Client
import cats.effect.IO
import cats.implicits._
import cats.effect.ContextShift

import org.http4s.{Header, Headers}
import org.http4s.Request
import org.http4s.EntityDecoder
import org.http4s.Method
import org.http4s.Uri
import org.http4s.EntityEncoder
import java.{util => ju}

import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.models.errors._

import com.hyperplan.infrastructure.serialization.tensorflow._
import com.hyperplan.infrastructure.serialization.rasa._
import com.hyperplan.infrastructure.logging.IOLogging

trait BackendService extends IOLogging {

  val blazeClient: Resource[IO, Client[IO]]

  def predictWithBackend(
      predictionId: String,
      project: Project,
      algorithm: Algorithm,
      features: Features.Features
  )(implicit cs: ContextShift[IO]): IO[Prediction] =
    (algorithm.backend, project) match {
      case (LocalClassification(preComputedLabels), _: ClassificationProject) =>
        IO.pure(
          ClassificationPrediction(
            predictionId,
            project.id,
            algorithm.id,
            features,
            Nil,
            preComputedLabels
          )
        )
      case (
          TensorFlowClassificationBackend(
            host,
            port,
            featuresTransformer,
            labelsTransformer
          ),
          classificationProject: ClassificationProject
          ) =>
        featuresTransformer
          .transform(features)
          .fold(
            err => IO.raiseError(err),
            transformedFeatures => {
              val uriString = s"http://${host}:${port}"
              buildRequestWithFeatures(
                uriString,
                algorithm.security.headers,
                transformedFeatures
              )(TensorFlowFeaturesSerializer.entityEncoder).fold(
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
                          )
                        )
                      case Left(err) =>
                        IO.raiseError(err)
                    }
              )
            }
          )
      case (
          RasaNluClassificationBackend(
            host,
            port,
            featuresTransformer,
            labelsTransformer
          ),
          classificationProject: ClassificationProject
          ) =>
        featuresTransformer
          .transform(features)
          .fold(
            err => IO.raiseError(err),
            transformedFeatures => {
              val uriString = s"http://${host}:${port}/parse"
              buildRequestWithFeatures(
                uriString,
                algorithm.security.headers,
                transformedFeatures
              )(RasaNluFeaturesSerializer.entityEncoder).fold(
                err => IO.raiseError(err),
                request =>
                  callHttpBackend(
                    request,
                    labelsTransformer.transform(
                      classificationProject.configuration.labels,
                      predictionId,
                      _: RasaNluClassificationLabels
                    )
                  )(RasaNluLabelsSerializer.entityDecoder).flatMap {
                    case Right(labels) =>
                      IO.pure(
                        ClassificationPrediction(
                          predictionId,
                          project.id,
                          algorithm.id,
                          features,
                          Nil,
                          labels
                        )
                      )
                    case Left(err) =>
                      IO.raiseError(err)
                  }
              )
            }
          )
      case (
          backend @ TensorFlowRegressionBackend(
            host,
            port,
            featuresTransformer
          ),
          regressionProject: RegressionProject
          ) =>
        featuresTransformer
          .transform(features)
          .fold(
            err => IO.raiseError(err),
            transformedFeatures => {
              val uriString = s"http://${host}:${port}"
              buildRequestWithFeatures(
                uriString,
                algorithm.security.headers,
                transformedFeatures
              )(TensorFlowFeaturesSerializer.entityEncoder).fold(
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
                          )
                        )
                      case Left(err) =>
                        logger.warn("An error occurred with the backend") >> IO
                          .raiseError(err)
                    }
              )
            }
          )
      case (backend, project) =>
        val errorMessage =
          s"Backend ${backend.getClass.getSimpleName} is not compatible with project ${project.getClass.getSimpleName}"
        logger.warn(errorMessage) >> IO.raiseError(new Exception(errorMessage))
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
