package com.foundaml.server.domain.services

import java.util.UUID

import cats.effect.{Async, Effect}
import cats.effect.IO
import cats.implicits._

import com.foundaml.server.domain.{FoundaMLConfig, models}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.models.events.{
  ClassificationPredictionEvent,
  PredictionEvent,
  RegressionPredictionEvent
}
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.repositories.{
  PredictionsRepository,
  ProjectsRepository
}
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.infrastructure.serialization.events.PredictionEventSerializer
import com.foundaml.server.infrastructure.streaming.{
  KinesisService,
  PubSubService,
  KafkaService
}
import doobie.free.connection.{AsyncConnectionIO, ConnectionIO}
import cats.effect.ContextShift

import cats.effect.Timer
class PredictionsService(
    predictionsRepository: PredictionsRepository,
    projectsService: ProjectsService,
    kinesisService: KinesisService,
    pubSubService: Option[PubSubService],
    kafkaService: Option[KafkaService],
    config: FoundaMLConfig
)(implicit cs: ContextShift[IO], timer: Timer[IO])
    extends IOLogging
    with TensorFlowBackendSupport {

  implicit val predictionEventEncoder = PredictionEventSerializer.encoder

  def persistClassificationPrediction(prediction: ClassificationPrediction) =
    predictionsRepository
      .insertClassificationPrediction(prediction)
      .flatMap(
        _.fold(
          err => logger.warn(err.getMessage) *> IO.raiseError(err),
          _ => IO.pure(prediction)
        )
      )
  def persistRegressionPrediction(prediction: RegressionPrediction) =
    predictionsRepository
      .insertRegressionPrediction(prediction)
      .flatMap(
        _.fold(
          err => logger.warn(err.getMessage) *> IO.raiseError(err),
          _ => IO.pure(prediction)
        )
      )

  def publishToStream(prediction: PredictionEvent): IO[Unit] =
    (for {
      _ <- pubSubService.fold[IO[Unit]](IO.unit)(_.publish(prediction))
      _ <- kafkaService.fold[IO[Unit]](IO.unit)(
        _.publish(prediction, prediction.projectId)
      )
      _ <- publishPredictionEventToKinesis(prediction)
    } yield ()).handleErrorWith {
      case err =>
        logger.warn(
          s"An occurred occurred when publishing data, ${err.getMessage}"
        ) *> IO.unit
    }

  def publishPredictionEventToKinesis(prediction: PredictionEvent) =
    if (config.kinesis.enabled) {
      kinesisService.put(
        prediction,
        config.kinesis.predictionsStream,
        prediction.projectId
      )(PredictionEventSerializer.encoder)
    } else IO.unit

  def noAlgorithm(): IO[Prediction] = {
    val message = "No algorithms are setup"
    logger.info(message).flatMap { _ =>
      IO.raiseError(
        NoAlgorithmAvailable(message)
      )
    }
  }

  def predictRegressionWithProjectPolicy(
      features: Features,
      project: RegressionProject
  ): IO[Prediction] =
    project.policy
      .take()
      .fold[IO[Prediction]] {
        val message = s"There is no algorithm in the project ${project.id}"
        logger.warn(message) *> IO.raiseError(
          NoAlgorithmAvailable(message)
        )
      } { algorithmId =>
        project.algorithmsMap
          .get(algorithmId)
          .fold[IO[Prediction]] {
            val message =
              s"The algorithm $algorithmId does not exist in the project ${project.id}"
            logger.debug(message) *> IO.raiseError(
              AlgorithmDoesNotExist(algorithmId)
            )
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
  ): IO[Prediction] =
    project.policy
      .take()
      .fold[IO[Prediction]] {
        val message = s"There is no algorithm in the project ${project.id}"
        logger.warn(message) *> IO.raiseError(
          NoAlgorithmAvailable(message)
        )
      } { algorithmId =>
        project.algorithmsMap
          .get(algorithmId)
          .fold[IO[Prediction]] {
            val message =
              s"The algorithm $algorithmId does not exist in the project ${project.id}"
            logger.debug(message) *> IO.raiseError(
              AlgorithmDoesNotExist(algorithmId)
            )
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
    IO.pure(
      ClassificationPrediction(
        UUID.randomUUID().toString,
        projectId,
        algorithm.id,
        features,
        List.empty,
        local.computed
      )
    )
  }

  import cats.effect.ContextShift
  def predictClassificationWithAlgorithm(
      project: ClassificationProject,
      algorithm: Algorithm,
      features: Features
  )(implicit cs: ContextShift[IO]): IO[ClassificationPrediction] = {
    val predictionIO = algorithm.backend match {
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
        IO.raiseError(
          IncompatibleBackend(
            "TensorFlowRegressionBackend can not do classification, use TensorFlowClassificationBackend instead"
          )
        )
    }
    predictionIO.flatMap { prediction =>
      persistClassificationPrediction(prediction) *> IO.pure(prediction)
    }
  }

  def predictRegressionWithAlgorithm(
      project: RegressionProject,
      algorithm: Algorithm,
      features: Features
  ): IO[RegressionPrediction] = {
    val predictionIO = algorithm.backend match {
      case local: LocalClassification =>
        IO.raiseError(
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
        IO.raiseError(
          IncompatibleBackend(
            "TensorFlowRegressionBackend can not do regression, use TensorFlowClassificationBackend instead"
          )
        )
    }
    predictionIO.flatMap { prediction =>
      persistRegressionPrediction(prediction) *> IO.pure(prediction)
    }
  }

  def validateFeatures(
      featuresConfiguration: FeaturesConfiguration,
      features: Features
  ) = featuresConfiguration match {
    case FeaturesConfiguration(
        id,
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
          case (FloatVectorFeature.featureClass, EmptyVectorFeature) => true
          case (IntVectorFeature.featureClass, IntVectorFeature(_)) => true
          case (IntVectorFeature.featureClass, EmptyVectorFeature) => true
          case (StringVectorFeature.featureClass, StringVectorFeature(_)) =>
            true
          case (StringVectorFeature.featureClass, EmptyVectorFeature) => true
          case (FloatVector2dFeature.featureClass, FloatVector2dFeature(_)) =>
            true
          case (FloatVector2dFeature.featureClass, EmptyVectorFeature) => true
          case (IntVector2dFeature.featureClass, IntVector2dFeature(_)) => true
          case (IntVector2dFeature.featureClass, EmptyVectorFeature) => true
          case (StringVector2dFeature.featureClass, StringVector2dFeature(_)) =>
            true
          case (StringVector2dFeature.featureClass, EmptyVector2dFeature) =>
            true
          case (config, feature) =>
            println(s"config $config does not match $feature")
            false
        }
        .reduce(_ & _)
      sameSize && sameClasses
  }

  def validateClassificationLabels(
      labelsConfiguration: LabelsConfiguration,
      labels: Set[ClassificationLabel]
  ): Boolean = labelsConfiguration.data match {
    case OneOfLabelsConfiguration(oneOf, _) =>
      oneOf == labels.map(_.label)
    case DynamicLabelsConfiguration(description) => true
  }

  def predict(
      projectId: String,
      features: Features,
      optionalAlgorithmId: Option[String]
  ): IO[Prediction] = projectsService.readProject(projectId).flatMap {
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
            .fold[IO[Prediction]](
              IO.raiseError(
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
                    IO.pure(prediction)
                  } else {
                    val message =
                      s"The labels do not match the configuration of project ${project.id}"
                    logger.warn(message) *> IO.raiseError(
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
      logger.warn(message) *> IO.raiseError(
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
            .fold[IO[Prediction]](
              IO.raiseError(
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
      logger.warn(message) *> IO.raiseError(
        FeaturesValidationFailed(
          message
        )
      )
    }
  }

  def addClassificationExample(
      labels: Option[String],
      predictionId: String,
      prediction: ClassificationPrediction
  ): ConnectionIO[ClassificationPredictionEvent] =
    labels.fold[ConnectionIO[ClassificationPredictionEvent]](
      AsyncConnectionIO.raiseError(IncorrectExample(Classification))
    )(
      label =>
        prediction.labels
          .find(_.label == label)
          .fold[ConnectionIO[ClassificationPredictionEvent]](
            AsyncConnectionIO.raiseError(LabelNotFound(label))
          )(
            label => {
              val example = label.label
              val examples = prediction.examples :+ example
              val predictionEvent = ClassificationPredictionEvent(
                UUID.randomUUID().toString,
                predictionId,
                prediction.projectId,
                prediction.algorithmId,
                prediction.features,
                prediction.labels,
                example
              )

              predictionsRepository
                .updateClassificationExamples(predictionId, examples)
                .map(_ => predictionEvent)
            }
          )
    )

  def addRegressionExample(
      valueOpt: Option[Float],
      predictionId: String,
      prediction: RegressionPrediction
  ) =
    valueOpt.fold[ConnectionIO[RegressionPredictionEvent]](
      AsyncConnectionIO.raiseError(IncorrectExample(Regression))
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
        predictionsRepository
          .updateRegressionExamples(predictionId, examples)
          .map(_ => predictionEvent)
      }
    )

  import cats.implicits._
  import doobie.free.connection._
  import cats.effect.implicits._

  def addExample(
      predictionId: String,
      labelOpt: Option[String],
      valueOpt: Option[Float]
  ) = {
    val transaction = for {
      prediction <- predictionsRepository.read(predictionId)
      event: PredictionEvent <- prediction match {
        case prediction: ClassificationPrediction =>
          addClassificationExample(labelOpt, predictionId, prediction)
        case prediction: RegressionPrediction =>
          addRegressionExample(valueOpt, predictionId, prediction)
      }
      _ <- AsyncConnectionIO.liftIO(
        Effect[IO].toIO(
          publishToStream(
            event
          )
        )
      )
    } yield event
    predictionsRepository.transact(transaction)
  }

}
