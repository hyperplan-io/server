package com.foundaml.server.domain.services

import java.util.UUID

import com.foundaml.server.domain.FoundaMLConfig
import com.foundaml.server.domain.factories.ProjectFactory
import org.http4s._
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabels
import com.foundaml.server.domain.repositories.{
  PredictionsRepository,
  ProjectsRepository
}
import com.foundaml.server.infrastructure.logging.IOLazyLogging
import com.foundaml.server.infrastructure.serialization.{
  PredictionSerializer,
  TensorFlowFeaturesSerializer,
  TensorFlowLabelsSerializer
}
import com.foundaml.server.infrastructure.streaming.KinesisService
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

class PredictionsService(
    projectsRepository: ProjectsRepository,
    predictionsRepository: PredictionsRepository,
    kinesisService: KinesisService,
    projectFactory: ProjectFactory,
    config: FoundaMLConfig
) extends IOLazyLogging {

  def persistPrediction(prediction: Prediction) =
    predictionsRepository
      .insert(prediction)
      .fold(
        err => warnLog(err.getMessage) *> Task.fail(err),
        _ => Task.succeed(prediction)
      )

  def publishPredictionToKinesis(prediction: Prediction) =
    if (config.kinesis.enabled) {
      kinesisService.put(
        prediction,
        config.kinesis.predictionsStream,
        prediction.projectId
      )(PredictionSerializer.encoder)
    } else Task.unit

  def noAlgorithm(): Task[Prediction] = {
    val message = "No algorithms are setup"
    infoLog(message).flatMap { _ =>
      Task.fail(
        NoAlgorithmAvailable(message)
      )
    }
  }

  def predictWithProjectPolicy(
      features: Features,
      project: Project
  ): Task[Prediction] =
    project.policy
      .take()
      .fold[Task[Prediction]] {
        val message = s"There is no algorithm in the project ${project.id}"
        warnLog(message) *> Task.fail(
          NoAlgorithmAvailable(message)
        )
      } { algorithmId =>
        project.algorithmsMap
          .get(algorithmId)
          .fold[Task[Prediction]] {
            val message =
              s"The algorithm $algorithmId does not exist in the project ${project.id}"
            debugLog(message) *> Task.fail(AlgorithmDoesNotExist(algorithmId))
          }(
            algorithm =>
              predictWithAlgorithm(
                project.id,
                algorithm,
                features
              )
          )
      }

  def predictWithLocalBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      local: Local
  ) = {
    Task.succeed(
      Prediction(
        UUID.randomUUID().toString,
        projectId,
        algorithm.id,
        features,
        local.computed,
        Set.empty
      )
    )
  }
  def predictWithTensorFlowBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      backend: TensorFlowBackend
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
          implicit val encoder
              : EntityEncoder[Task, TensorFlowClassificationFeatures] =
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
                    .withBody(tfFeatures)
                BlazeClientBuilder[Task](ExecutionContext.global).resource
                  .use(
                    _.expect[TensorFlowLabels](request)(
                      TensorFlowLabelsSerializer.entityDecoder
                    ).flatMap { tfLabels =>
                        backend.labelsTransformer
                          .transform(predictionId, tfLabels)
                          .fold(
                            err => Task.fail(err),
                            labels =>
                              Task.succeed(
                                Prediction(
                                  predictionId,
                                  projectId,
                                  algorithm.id,
                                  features,
                                  labels,
                                  Set.empty
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

  def predictWithAlgorithm(
      projectId: String,
      algorithm: Algorithm,
      features: Features
  ): Task[Prediction] = {
    val predictionTask = algorithm.backend match {
      case local: Local =>
        predictWithLocalBackend(projectId, algorithm, features, local)
      case tfBackend: TensorFlowBackend =>
        predictWithTensorFlowBackend(projectId, algorithm, features, tfBackend)
    }
    predictionTask.flatMap { prediction =>
      persistPrediction(prediction) *> publishPredictionToKinesis(prediction) *> Task
        .succeed(prediction)
    }
  }

  def validateFeatures(
      featuresConfiguration: FeaturesConfiguration,
      features: Features
  ) = featuresConfiguration match {
    case FeaturesConfiguration(
        featuresConfigList: List[FeatureConfiguration]
        ) =>
      lazy val sameSize = features.size == featuresConfigList.size
      lazy val sameClasses = featuresConfigList
        .map(_.featuresType)
        .zip(features)
        .map {
          case (FloatFeature.featureClass, FloatFeature(_)) => true
          case (IntFeature.featureClass, IntFeature(_)) => true
          case (StringFeature.featureClass, StringFeature(_)) => true
          case (FloatVectorFeature.featureClass, FloatVectorFeature(_)) => true
          case (IntVectorFeature.featureClass, IntVectorFeature(_)) => true
          case (StringVectorFeature.featureClass, StringVectorFeature(_)) =>
            true
          case _ => false
        }
        .reduce(_ & _)

      sameSize && sameClasses
  }

  def validateLabels(
      expectedLabelsClass: Set[String],
      labels: Labels
  ): Boolean = {
    expectedLabelsClass == labels.labels.map(_.label)
  }

  def predict(
      projectId: String,
      features: Features,
      optionalAlgorithmId: Option[String]
  ) = projectFactory.get(projectId).flatMap {
    case project: ClassificationProject =>
      predictForClassificationProject(project, features, optionalAlgorithmId)
  }

  def predictForClassificationProject(
      project: ClassificationProject,
      features: Features,
      optionalAlgorithmId: Option[String]
  ) = {

    if (validateFeatures(
        project.configuration.features,
        features
      )) {
      optionalAlgorithmId.fold(
        predictWithProjectPolicy(features, project)
      )(
        algorithmId =>
          project.algorithmsMap
            .get(algorithmId)
            .fold[Task[Prediction]](
              Task.fail(
                AlgorithmDoesNotExist(algorithmId)
              )
            )(
              algorithm =>
                predictWithAlgorithm(project.id, algorithm, features).flatMap {
                  prediction =>
                    if (validateLabels(
                        project.configuration.labels,
                        prediction.labels
                      )) {
                      Task.succeed(prediction)
                    } else {
                      val message =
                        s"The labels do not match the configuration of project ${project.id}"
                      warnLog(message) *> Task.fail(
                        LabelsValidationFailed(
                          message
                        )
                      )
                    }
                }
            )
      )
    } else {
      val message =
        s"The features do not match the configuration of project ${project.id}"
      warnLog(message) *> Task.fail(
        FeaturesValidationFailed(
          message
        )
      )
    }
  }

  def addExample(predictionId: String, labelId: String) =
    predictionsRepository.read(predictionId).flatMap { prediction =>
      prediction.labels.labels
        .find(_.id == labelId)
        .fold[Task[Label]](
          Task.fail(LabelNotFound(labelId))
        )(
          label => {
            val examples = prediction.examples + label.id

            predictionsRepository
              .updateExamples(predictionId, examples) *> kinesisService.put(
              examples,
              config.kinesis.examplesStream,
              predictionId
            ) *> Task.succeed(label)
          }
        )
    }
}
