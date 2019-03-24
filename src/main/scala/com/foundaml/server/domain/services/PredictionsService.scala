package com.foundaml.server.domain.services

import java.util.UUID

import com.foundaml.server.domain.{FoundaMLConfig, models}
import com.foundaml.server.domain.factories.{PredictionFactory, ProjectFactory}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.repositories.{PredictionsRepository, ProjectsRepository}
import com.foundaml.server.infrastructure.logging.IOLazyLogging
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.infrastructure.streaming.KinesisService
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class PredictionsService(
                          projectsRepository: ProjectsRepository,
                          predictionsRepository: PredictionsRepository,
                          kinesisService: KinesisService,
                          projectFactory: ProjectFactory,
                          predictionFactory: PredictionFactory,
                          config: FoundaMLConfig
) extends IOLazyLogging with TensorFlowBackendSupport {

  def persistClassificationPrediction(prediction: ClassificationPrediction) =
    predictionsRepository
      .insertClassificationPrediction(prediction)
      .fold(
        err => warnLog(err.getMessage) *> Task.fail(err),
        _ => Task.succeed(prediction)
      )
  def persistRegressionPrediction(prediction: RegressionPrediction) =
    predictionsRepository
      .insertRegressionPrediction(prediction)
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
              predictClassificationWithAlgorithm(
                project.id,
                algorithm,
                features
              )
          )
      }

  def predictWithLocalClassificationBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      local: LocalClassification
  ) = {
    Task.succeed(
      ClassificationPrediction(
        UUID.randomUUID().toString,
        projectId,
        algorithm.id,
        features,
        Set.empty,
        local.computed
      )
    )
  }

  def predictClassificationWithAlgorithm(
      projectId: String,
      algorithm: Algorithm,
      features: Features
  ): Task[ClassificationPrediction] = {
    val predictionTask = algorithm.backend match {
      case local: LocalClassification =>
        predictWithLocalClassificationBackend(projectId, algorithm, features, local)
      case tfBackend: TensorFlowClassificationBackend =>
        predictWithTensorFlowClassificationBackend(projectId, algorithm, features, tfBackend)
      case tfBackend: TensorFlowRegressionBackend =>
        Task.fail(IncompatibleBackend("TensorFlowRegressionBackend can not do regression, use TensorFlowClassificationBackend instead"))
    }
    predictionTask.flatMap { prediction =>
      persistClassificationPrediction(prediction) *> publishPredictionToKinesis(prediction) *> Task
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

  def validateClassificationLabels(
      expectedLabelsClass: Set[String],
      labels: Set[ClassificationLabel]
  ): Boolean = {
    expectedLabelsClass == labels.map(_.label)
  }

  def predict(
      projectId: String,
      features: Features,
      optionalAlgorithmId: Option[String]
  ) = projectFactory.get(projectId).flatMap {
    case project: ClassificationProject =>
      predictForClassificationProject(project, features, optionalAlgorithmId)
    case project: RegressionProject =>
      predictForRegressionProject(project, features, optionalAlgorithmId)
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
                predictClassificationWithAlgorithm(project.id, algorithm, features).flatMap {
                  prediction =>
                    if (validateClassificationLabels(
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

  def predictForRegressionProject(
      project: RegressionProject,
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
              algorithm => predictClassificationWithAlgorithm(project.id, algorithm, features)
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
    predictionFactory.get(predictionId).flatMap {
      case prediction: ClassificationPrediction =>
        prediction.labels
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
      case prediction: RegressionPrediction => ???
    }
}
