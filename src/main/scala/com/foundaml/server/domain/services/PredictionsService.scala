package com.foundaml.server.domain.services

import java.util.UUID

import com.foundaml.server.domain.{FoundaMLConfig, models}
import com.foundaml.server.domain.factories.{PredictionFactory, ProjectFactory}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.models.events.{ClassificationPredictionEvent, PredictionEvent, RegressionPredictionEvent}
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.repositories.{PredictionsRepository, ProjectsRepository}
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.infrastructure.serialization.events.PredictionEventSerializer
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
) extends IOLogging
    with TensorFlowBackendSupport {

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

  def publishPredictionEventToKinesis(prediction: PredictionEvent) =
    if (config.kinesis.enabled) {
      kinesisService.put(
        prediction,
        config.kinesis.predictionsStream,
        prediction.projectId
      )(PredictionEventSerializer.encoder)
    } else Task.unit

  def noAlgorithm(): Task[Prediction] = {
    val message = "No algorithms are setup"
    infoLog(message).flatMap { _ =>
      Task.fail(
        NoAlgorithmAvailable(message)
      )
    }
  }

  def predictRegressionWithProjectPolicy(
      features: Features,
      project: RegressionProject
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
              predictRegressionWithAlgorithm(
                project,
                algorithm,
                features
              )
          )
      }

  def predictClassificationWithProjectPolicy(
      features: Features,
      project: ClassificationProject
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
                project,
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
      project: ClassificationProject,
      algorithm: Algorithm,
      features: Features
  ): Task[ClassificationPrediction] = {
    val predictionTask = algorithm.backend match {
      case local: LocalClassification =>
        predictWithLocalClassificationBackend(
          project.id,
          algorithm,
          features,
          local
        )
      case tfBackend: TensorFlowClassificationBackend =>
        predictWithTensorFlowClassificationBackend(
          project.id,
          algorithm,
          features,
          tfBackend,
          project.configuration.labels
        )
      case tfBackend: TensorFlowRegressionBackend =>
        Task.fail(
          IncompatibleBackend(
            "TensorFlowRegressionBackend can not do classification, use TensorFlowClassificationBackend instead"
          )
        )
    }
    predictionTask.flatMap { prediction =>
      persistClassificationPrediction(prediction) *> Task
        .succeed(prediction)
    }
  }

  def predictRegressionWithAlgorithm(
      project: RegressionProject,
      algorithm: Algorithm,
      features: Features
  ): Task[RegressionPrediction] = {
    val predictionTask = algorithm.backend match {
      case local: LocalClassification =>
        Task.fail(
          IncompatibleBackend("LocalClassification can not do regression")
        )
      case tfBackend: TensorFlowRegressionBackend =>
        predictWithTensorFlowRegressionBackend(
          project.id,
          algorithm,
          features,
          tfBackend
        )
      case tfBackend: TensorFlowClassificationBackend =>
        Task.fail(
          IncompatibleBackend(
            "TensorFlowRegressionBackend can not do regression, use TensorFlowClassificationBackend instead"
          )
        )
    }
    predictionTask.flatMap { prediction =>
      persistRegressionPrediction(prediction) *> Task
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
      labelsConfiguration: LabelsConfiguration,
      labels: Set[ClassificationLabel]
  ): Boolean = labelsConfiguration match {
    case OneOfLabelsConfiguration(oneOf, _) =>
      oneOf == labels.map(_.label)
    case DynamicLabelsConfiguration(description) => true
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
        predictClassificationWithProjectPolicy(features, project)
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
                predictClassificationWithAlgorithm(
                  project,
                  algorithm,
                  features
                ).flatMap { prediction =>
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
        predictRegressionWithProjectPolicy(features, project)
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
                predictRegressionWithAlgorithm(project, algorithm, features)
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

  def addExample(
      predictionId: String,
      labelOpt: Option[String],
      valueOpt: Option[Float]
  ) =
    predictionFactory.get(predictionId).flatMap {
      case prediction: ClassificationPrediction =>
        labelOpt.fold[Task[Label]](
          Task.fail(IncorrectExample(Classification))
        )(
          label =>
            prediction.labels
              .find(_.label == label)
              .fold[Task[Label]](
                Task.fail(LabelNotFound(label))
              )(
                label => {
                  val example = label.label
                  val examples = prediction.examples + example
                  val predictionEvent = ClassificationPredictionEvent(
                    UUID.randomUUID().toString,
                    predictionId,
                    prediction.projectId,
                    prediction.algorithmId,
                    prediction.features,
                    prediction.labels,
                    example
                  )
                  println(PredictionEventSerializer.encodeJsonNoSpaces(predictionEvent))
                  predictionsRepository
                    .updateClassificationExamples(predictionId, examples) *>
                    publishPredictionEventToKinesis(
                      predictionEvent
                    ) *> Task
                    .succeed(label)
                }
              )
        )

      case prediction: RegressionPrediction =>
        valueOpt.fold[Task[Label]](
          Task.fail(IncorrectExample(Regression))
        )(
          value => {
            val example = value
            val examples = prediction.examples :+ example

            val predictionEvent = RegressionPredictionEvent(
              UUID.randomUUID().toString,
              predictionId,
              prediction.projectId,
              prediction.algorithmId,
              prediction.features,
              prediction.labels,
              example
            )
            println(PredictionEventSerializer.encodeJsonNoSpaces(predictionEvent))

            predictionsRepository
              .updateRegressionExamples(predictionId, examples) *>
              publishPredictionEventToKinesis(
                predictionEvent
              ) *>
              Task.succeed(prediction.labels.head)
          }
        )

    }
}
