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
import scalaz.zio.Task
import scalaz.zio.interop.catz._

import scala.concurrent.ExecutionContext

trait TensorFlowBackendSupport extends IOLogging {

  def predictWithTensorFlowClassificationBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      backend: TensorFlowClassificationBackend,
      labelsConfiguration: LabelsConfiguration
  ) = {
    val predictionId = UUID.randomUUID().toString
    backend.featuresTransformer
      .transform(features)
      .fold(
        err =>
          warnLog(err.getMessage) *>
            Task.fail(
              FeaturesTransformerError(
                "The features could not be transformed to a TensorFlow compatible format"
              )
            ),
        tfFeatures => {
          implicit val encoder: EntityEncoder[Task, TensorFlowFeatures] =
            TensorFlowFeaturesSerializer.entityEncoder
          val uriString = s"http://${backend.host}:${backend.port}"
          Uri
            .fromString(uriString)
            .fold(
              _ =>
                Task.fail(
                  InvalidArgument(
                    s"The following uri could not be parsed, check your backend configuration. $uriString"
                  )
                ),
              uri => {
                val request =
                  Request[Task](method = Method.POST, uri = uri)
                    .withEntity(tfFeatures)
                BlazeClientBuilder[Task](ExecutionContext.global).resource
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
                            err => Task.fail(err),
                            labels =>
                              Task.succeed(
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
                      .catchAll { err =>
                        {
                          val message =
                            s"An error occurred with backend: ${err.getMessage}"
                          errorLog(message) *> Task.fail(BackendError(message))
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
  ): Task[RegressionPrediction] = {
    val predictionId = UUID.randomUUID().toString
    backend.featuresTransformer
      .transform(features)
      .fold(
        err =>
          warnLog(err.getMessage) *>
            Task.fail(
              FeaturesTransformerError(
                "The features could not be transformed to a TensorFlow compatible format"
              )
            ),
        tfFeatures => {
          implicit val encoder: EntityEncoder[Task, TensorFlowFeatures] =
            TensorFlowFeaturesSerializer.entityEncoder
          val uriString = s"http://${backend.host}:${backend.port}"
          Uri
            .fromString(uriString)
            .fold(
              _ =>
                Task.fail(
                  InvalidArgument(
                    s"The following uri could not be parsed, check your backend configuration. $uriString"
                  )
                ),
              uri => {
                val request =
                  Request[Task](method = Method.POST, uri = uri)
                    .withEntity(tfFeatures)
                BlazeClientBuilder[Task](ExecutionContext.global).resource
                  .use(
                    _.expect[TensorFlowRegressionLabels](request)(
                      TensorFlowRegressionLabelsSerializer.entityDecoder
                    ).flatMap {
                        tfLabels =>
                          tfLabels.result.headOption
                            .fold[Task[RegressionPrediction]](
                              Task.fail(
                                BackendError(
                                  "Unexpected result from TensorFlow"
                                )
                              )
                            ) {
                              tfLabel =>
                                tfLabel.headOption
                                  .fold[Task[RegressionPrediction]](
                                    Task.fail(
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
                                    Task.succeed(prediction)
                                  }
                            }
                      }
                      .catchAll { err =>
                        {
                          val message =
                            s"An error occurred with backend: ${err.getMessage}"
                          errorLog(message) *> Task.fail(BackendError(message))
                        }
                      }
                  )
              }
            )

        }
      )
  }
}
