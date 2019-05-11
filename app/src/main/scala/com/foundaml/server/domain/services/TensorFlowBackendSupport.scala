package com.foundaml.server.domain.services

import java.util.UUID

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends.{
  TensorFlowClassificationBackend,
  TensorFlowRegressionBackend
}
import com.foundaml.server.domain.models.errors.{
  BackendError,
  FeaturesTransformerError,
  InvalidArgument
}
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features.TensorFlowFeatures
import com.foundaml.server.domain.models.labels.{
  Labels,
  RegressionLabel,
  TensorFlowClassificationLabels,
  TensorFlowRegressionLabels
}
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.serialization.tensorflow.{
  TensorFlowClassificationLabelsSerializer,
  TensorFlowFeaturesSerializer,
  TensorFlowRegressionLabelsSerializer
}
import org.http4s.{EntityEncoder, Method, Request, Uri}
import org.http4s.client.blaze.BlazeClientBuilder
import cats.effect.IO
import cats.implicits._

import scala.concurrent.ExecutionContext

trait TensorFlowBackendSupport extends IOLogging {

  import cats.effect.ContextShift
  def predictWithTensorFlowClassificationBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      backend: TensorFlowClassificationBackend,
      labelsConfiguration: LabelsConfiguration
  )(implicit cs: ContextShift[IO]) = {
    val predictionId = UUID.randomUUID().toString
    backend.featuresTransformer
      .transform(features)
      .fold(
        err =>
          logger.warn(err.getMessage) *>
            IO.raiseError(
              FeaturesTransformerError(
                "The features could not be transformed to a TensorFlow compatible format"
              )
            ),
        tfFeatures => {
          implicit val encoder: EntityEncoder[IO, TensorFlowFeatures] =
            TensorFlowFeaturesSerializer.entityEncoder
          val uriString = s"http://${backend.host}:${backend.port}"
          Uri
            .fromString(uriString)
            .fold(
              _ =>
                IO.raiseError(
                  InvalidArgument(
                    s"The following uri could not be parsed, check your backend configuration. $uriString"
                  )
                ),
              uri => {
                import org.http4s.{Header, Headers}
                val request =
                  Request[IO](method = Method.POST, uri = uri)
                    .withHeaders(Headers(algorithm.security.headers.map {
                      case (key, value) => Header(key, value)
                    }))
                    .withEntity(tfFeatures)
                BlazeClientBuilder[IO](ExecutionContext.global).resource
                  .use(
                    _.expect[TensorFlowClassificationLabels](request)(
                      TensorFlowClassificationLabelsSerializer.entityDecoder
                    ).flatMap { tfLabels =>
                        backend.labelsTransformer
                          .transform(
                            labelsConfiguration,
                            predictionId,
                            tfLabels
                          )
                          .fold(
                            err => IO.raiseError(err),
                            labels =>
                              IO.pure(
                                ClassificationPrediction(
                                  predictionId,
                                  projectId,
                                  algorithm.id,
                                  features,
                                  List.empty,
                                  labels
                                )
                              )
                          )
                      }
                      .handleErrorWith { err =>
                        {
                          val message =
                            s"An error occurred with backend: ${err.getMessage}"
                          logger.error(message) *> IO
                            .raiseError(BackendError(message))
                        }
                      }
                  )
              }
            )

        }
      )
  }

  def predictWithTensorFlowRegressionBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      backend: TensorFlowRegressionBackend
  )(implicit cs: ContextShift[IO]): IO[RegressionPrediction] = {
    val predictionId = UUID.randomUUID().toString
    backend.featuresTransformer
      .transform(features)
      .fold(
        err =>
          logger.warn(err.getMessage) *>
            IO.raiseError(
              FeaturesTransformerError(
                "The features could not be transformed to a TensorFlow compatible format"
              )
            ),
        tfFeatures => {
          implicit val encoder: EntityEncoder[IO, TensorFlowFeatures] =
            TensorFlowFeaturesSerializer.entityEncoder
          val uriString = s"http://${backend.host}:${backend.port}"
          Uri
            .fromString(uriString)
            .fold(
              _ =>
                IO.raiseError(
                  InvalidArgument(
                    s"The following uri could not be parsed, check your backend configuration. $uriString"
                  )
                ),
              uri => {
                val request =
                  Request[IO](method = Method.POST, uri = uri)
                    .withEntity(tfFeatures)
                BlazeClientBuilder[IO](ExecutionContext.global).resource
                  .use(
                    _.expect[TensorFlowRegressionLabels](request)(
                      TensorFlowRegressionLabelsSerializer.entityDecoder
                    ).flatMap {
                        tfLabels =>
                          tfLabels.result.headOption
                            .fold[IO[RegressionPrediction]](
                              IO.raiseError(
                                BackendError(
                                  "Unexpected result from TensorFlow"
                                )
                              )
                            ) {
                              tfLabel =>
                                tfLabel.headOption
                                  .fold[IO[RegressionPrediction]](
                                    IO.raiseError(
                                      BackendError(
                                        "Unexpected result from TensorFlow"
                                      )
                                    )
                                  ) { tfLabel =>
                                    val prediction = RegressionPrediction(
                                      predictionId,
                                      projectId,
                                      algorithm.id,
                                      features,
                                      Nil,
                                      Set(
                                        RegressionLabel(
                                          tfLabel,
                                          ExampleUrlService
                                            .correctRegressionExampleUrl(
                                              predictionId
                                            )
                                        )
                                      )
                                    )
                                    IO.pure(prediction)
                                  }
                            }
                      }
                      .handleErrorWith { err =>
                        {
                          val message =
                            s"An error occurred with backend: ${err.getMessage}"
                          logger.error(message) *> IO
                            .raiseError(BackendError(message))
                        }
                      }
                  )
              }
            )

        }
      )
  }
}
