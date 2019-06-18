package com.hyperplan.domain.services

import java.util.UUID

import cats.effect.{Async, Effect}
import cats.effect.IO
import cats.implicits._
import cats.effect.ContextShift

import com.hyperplan.application.ApplicationConfig

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models.errors._
import com.hyperplan.domain.models.events.{
  ClassificationPredictionEvent,
  PredictionEvent,
  RegressionPredictionEvent
}
import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.repositories.{
  PredictionsRepository,
  ProjectsRepository
}
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization.PredictionSerializer
import com.hyperplan.infrastructure.serialization.events.PredictionEventSerializer
import com.hyperplan.infrastructure.streaming.{
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
    config: ApplicationConfig
)(implicit cs: ContextShift[IO], timer: Timer[IO])
    extends IOLogging
    with TensorFlowBackendSupport {

  implicit val predictionEventEncoder = PredictionEventSerializer.encoder

  def persistClassificationPrediction(
      prediction: ClassificationPrediction,
      entityLinks: List[EntityLink]
  ): IO[Either[PredictionError, Int]] =
    predictionsRepository.transact(
      for {
        count <- predictionsRepository.insertClassificationPrediction(
          prediction
        )
        links = entityLinks.map(link => link.name -> link.id)
        _ <- predictionsRepository.insertEntityLink(prediction.id, links)
      } yield count
    )

  def persistRegressionPrediction(
      prediction: RegressionPrediction,
      entityLinks: List[EntityLink]
  ): IO[Either[PredictionError, Int]] =
    predictionsRepository.transact(
      for {
        count <- predictionsRepository.insertRegressionPrediction(
          prediction
        )
        links = entityLinks.map(link => link.name -> link.id)
        _ <- predictionsRepository.insertEntityLink(prediction.id, links)
      } yield count
    )

  def publishToStream(prediction: PredictionEvent, streamConfiguration: Option[StreamConfiguration]): IO[Unit] =
    (for {
      _ <- pubSubService.fold[IO[Unit]](IO.unit)(
        _.publish(prediction, streamConfiguration.fold(config.gcp.pubsub.predictionsTopicId)(_.topic))
      )
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
      project: RegressionProject,
      entityLinks: List[EntityLink]
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
                features,
                entityLinks
              )
          )
      }

  def predictClassificationWithProjectPolicy(
      features: Features,
      project: ClassificationProject,
      entityLinks: List[EntityLink]
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
                features,
                entityLinks
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

  def predictClassificationWithAlgorithm(
      project: ClassificationProject,
      algorithm: Algorithm,
      features: Features,
      entityLinks: List[EntityLink]
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
      if (config.prediction.storeInPostgresql) {
        logger.debug(
          s"storing prediction ${prediction.id} in postgresql"
        ) *> persistClassificationPrediction(prediction, entityLinks) *> IO
          .pure(
            prediction
          )
      } else {
        logger.debug(
          s"storing predictions in postgresql in disabled, ignoring ${prediction.id}"
        ) *> IO.pure(prediction)
      }
    }
  }

  def predictRegressionWithAlgorithm(
      project: RegressionProject,
      algorithm: Algorithm,
      features: Features,
      entityLinks: List[EntityLink]
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
      if (config.prediction.storeInPostgresql) {
        logger.debug(
          s"storing prediction ${prediction.id} in postgresql"
        ) *> persistRegressionPrediction(prediction, entityLinks) *> IO.pure(
          prediction
        )
      } else {
        logger.debug(
          s"storing predictions in postgresql in disabled, ignoring ${prediction.id}"
        ) *> IO.pure(prediction)
      }
    }
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
      entityLinks: List[EntityLink],
      optionalAlgorithmId: Option[String]
  ): IO[Prediction] = projectsService.readProject(projectId).flatMap {
    case project: ClassificationProject =>
      predictForClassificationProject(
        project,
        features,
        entityLinks,
        optionalAlgorithmId
      )
    case project: RegressionProject =>
      predictForRegressionProject(
        project,
        features,
        entityLinks,
        optionalAlgorithmId
      )
  }

  def predictForClassificationProject(
      project: ClassificationProject,
      features: Features,
      entityLinks: List[EntityLink],
      optionalAlgorithmId: Option[String]
  ) = {
    optionalAlgorithmId.fold(
      predictClassificationWithProjectPolicy(features, project, entityLinks)
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
                features,
                entityLinks
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
  }

  def predictForRegressionProject(
      project: RegressionProject,
      features: Features,
      entityLinks: List[EntityLink],
      optionalAlgorithmId: Option[String]
  ) =
    optionalAlgorithmId.fold(
      predictRegressionWithProjectPolicy(features, project, entityLinks)
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
              predictRegressionWithAlgorithm(
                project,
                algorithm,
                features,
                entityLinks
              )
          )
    )

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
      project <- AsyncConnectionIO.liftIO(
        Effect[IO].toIO(
          projectsService.readProject(prediction.projectId) 
        )
      )
      event: PredictionEvent <- prediction match {
        case prediction: ClassificationPrediction =>
          addClassificationExample(labelOpt, predictionId, prediction)
        case prediction: RegressionPrediction =>
          addRegressionExample(valueOpt, predictionId, prediction)
      }
      _ <- AsyncConnectionIO.liftIO(
        Effect[IO].toIO(
          publishToStream(
            event,
            project.configuration.streamConfiguration
          )
        )
      )
    } yield event
    predictionsRepository.transact(transaction)
  }

}
