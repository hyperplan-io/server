package com.hyperplan.domain.services

import cats.effect.Resource
import org.http4s.client.Client
import cats.effect.IO
import cats.effect.ContextShift

import org.http4s.{Header, Headers}

import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.models.Algorithm
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models.LabelsConfiguration
import com.hyperplan.domain.models.Prediction
import org.http4s.Request
import org.http4s.EntityDecoder
import org.http4s.Method
import org.http4s.Uri
import org.http4s.EntityEncoder
import com.hyperplan.infrastructure.serialization.tensorflow.TensorFlowFeaturesSerializer
import com.hyperplan.domain.models.labels.TensorFlowClassificationLabels
import com.hyperplan.infrastructure.serialization.tensorflow.TensorFlowClassificationLabelsSerializer
import com.hyperplan.domain.models.labels.Labels
import java.{util => ju}
import com.hyperplan.domain.models.errors.LabelsTransformerError
import com.hyperplan.domain.models.labels.Label
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.models.labels.TensorFlowRegressionLabels
import com.hyperplan.domain.models.labels.RegressionLabel
import com.hyperplan.infrastructure.serialization.tensorflow.TensorFlowRegressionLabelsSerializer
import com.hyperplan.domain.models.RegressionPrediction
import com.hyperplan.domain.models.ClassificationPrediction
import com.hyperplan.domain.models.Project
import com.hyperplan.domain.models.ClassificationProject
import com.hyperplan.domain.models.RegressionProject

trait BackendService extends IOLogging {

  val blazeClient: Resource[IO, Client[IO]]

  def predictWithBackend(
      project: Project,
      algorithm: Algorithm,
      features: Features.Features
  )(implicit cs: ContextShift[IO]): IO[Prediction] =
    (algorithm.backend, project) match {
      case (LocalClassification(computed), _: ClassificationProject) =>
        ???
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
                      ju.UUID.randomUUID().toString,
                      _: TensorFlowClassificationLabels
                    )
                  )(TensorFlowClassificationLabelsSerializer.entityDecoder)
                    .flatMap {
                      case Right(labels) =>
                        val predictionId = ju.UUID.randomUUID.toString
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
          RasaNluClassificationBackend(_, _, _, _),
          _: ClassificationProject
          ) =>
        ???
      case (_: TensorFlowRegressionBackend, _: RegressionProject) =>
        IO.raiseError(new Exception(""))
      case _ => ???
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
