package com.hyperplan.domain.services

import java.util.UUID

import cats.effect.{Async, Effect}
import cats.effect.IO
import cats.implicits._
import cats.effect.ContextShift
import doobie.free.connection._

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
import cats.effect.Resource
import org.http4s.client.Client

class PredictionsService(
    predictionsRepository: PredictionsRepository,
    projectsService: ProjectsService,
    kinesisService: Option[KinesisService],
    pubSubService: Option[PubSubService],
    kafkaService: Option[KafkaService],
    val blazeClient: Resource[IO, Client[IO]],
    config: ApplicationConfig
)(implicit cs: ContextShift[IO], timer: Timer[IO])
    extends IOLogging
    with BackendService {

  implicit val predictionEventEncoder = PredictionEventSerializer.encoder

  def persistPrediction(
    prediction: Prediction,
    entityLinks: List[EntityLink]
  ): IO[Either[PredictionError, Int]] = ???

  def persistClassificationPrediction(
      prediction: ClassificationPrediction,
      entityLinks: List[EntityLink]
  ): IO[Either[PredictionError, Int]] = predictionsRepository.transact(
    for {
      count <- prediction match {
        case classificationPrediction: ClassificationPrediction =>
          predictionsRepository.insertClassificationPrediction(
            classificationPrediction
          )
        case regressionPrediction: RegressionPrediction =>
          predictionsRepository.insertRegressionPrediction(regressionPrediction)
      }
      links = entityLinks.map(link => link.name -> link.id)
      _ <- predictionsRepository.insertEntityLink(prediction.id, links)
    } yield count
  )

  def publishToStream(
      prediction: PredictionEvent,
      streamConfiguration: Option[StreamConfiguration]
  ): IO[Unit] =
    (for {
      _ <- pubSubService.fold[IO[Seq[String]]](IO.pure(Seq.empty))(
        _.publish(
          prediction,
          streamConfiguration
            .fold(config.gcp.pubsub.predictionsTopicId)(_.topic)
        )
      )
      _ <- kafkaService.fold[IO[Unit]](IO.unit)(
        _.publish(prediction, prediction.projectId)
      )
      _ <- kinesisService.fold[IO[Unit]](IO.unit)(
        _.put(
          prediction,
          streamConfiguration.fold(
            config.kinesis.predictionsStream
          )(_.topic),
          prediction.projectId
        )
      )
    } yield ()).handleErrorWith {
      case err =>
        logger.warn(
          s"An occurred occurred when publishing data, ${err.getMessage}"
        ) *> IO.unit
    }

  def noAlgorithm(): IO[Prediction] = {
    val message = "No algorithms are setup"
    logger.info(message).flatMap { _ =>
      IO.raiseError(
        NoAlgorithmAvailable(message)
      )
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
    ): IO[Prediction] = projectsService.readProject(projectId).flatMap { project =>
      val maybeAlgorithmId = optionalAlgorithmId.fold(
        (project.policy.take)
      )(algorithmId => algorithmId.some)
      maybeAlgorithmId.fold(IO.raiseError(NoAlgorithmAvailable(""))){ algorithmId =>
        for {
          algorithm <- project.algorithmsMap.get(algorithmId).fold[IO[Algorithm]](IO.raiseError(AlgorithmDoesNotExist("")))(algorithm => IO.pure(algorithm))
          prediction <- predictWithBackend(
            project,
            algorithm,
            features
          )
          _ <- if (config.prediction.storeInPostgresql) {
            logger.debug(
              s"storing prediction ${prediction.id} in postgresql"
            ) *> persistPrediction(prediction, entityLinks) *> IO
              .pure(
                prediction
              )
          } else {
            logger.debug(
              s"storing predictions in postgresql in disabled, ignoring ${prediction.id}"
            ) *> IO.pure(prediction)
          }
        } yield ???
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
            project.configuration.dataStream
          )
        )
      )
    } yield event
    predictionsRepository.transact(transaction)
  }

}
