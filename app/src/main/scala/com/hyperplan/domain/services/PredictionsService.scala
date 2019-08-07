package com.hyperplan.domain.services

import java.{util => ju}

import cats.effect.{Effect, IO}
import cats.implicits._
import cats.effect.ContextShift
import cats.effect.Timer
import cats.effect.Resource

import doobie.free.connection.{AsyncConnectionIO, ConnectionIO}

import org.http4s.client.Client

import com.hyperplan.application.ApplicationConfig

import com.hyperplan.domain.models._
import com.hyperplan.domain.errors.PredictionError
import com.hyperplan.domain.errors.PredictionError._
import com.hyperplan.domain.models.events._
import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.labels._
import com.hyperplan.domain.repositories.PredictionsRepository

import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization.events.PredictionEventSerializer
import com.hyperplan.infrastructure.streaming.{
  KafkaService,
  KinesisService,
  PubSubService
}

class PredictionsService(
    predictionsRepository: PredictionsRepository,
    projectsService: ProjectsService,
    backendService: BackendService,
    kinesisService: Option[KinesisService],
    pubSubService: Option[PubSubService],
    kafkaService: Option[KafkaService],
    config: ApplicationConfig
)(implicit cs: ContextShift[IO], timer: Timer[IO])
    extends IOLogging{

  implicit val predictionEventEncoder = PredictionEventSerializer.encoder

  def persistPrediction(
      prediction: Prediction,
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
      _ <- logger.info("publishing in stream")
      _ <- pubSubService.fold[IO[Seq[String]]](IO.pure(Seq.empty))(
        _.publish(
          prediction,
          streamConfiguration
            .fold(config.gcp.pubsub.predictionsTopicId)(_.topic)
        )
      )
      _ <- kafkaService.fold[IO[Unit]](IO.unit)(
        _.publish(
          prediction,
          prediction.projectId,
          streamConfiguration.map(_.topic)
        )
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
    } yield ()).handleErrorWith { err =>
      logger.warn(
        s"An occurred occurred when publishing data, ${err.getMessage}"
      ) *> IO.unit
    }

  def validateClassificationLabels(
      labelsConfiguration: LabelVectorDescriptor,
      labels: Set[ClassificationLabel]
  ): Boolean = labelsConfiguration.data match {
    case OneOfLabelsDescriptor(oneOf, _) =>
      oneOf == labels.map(_.label)
    case DynamicLabelsDescriptor(description) => true
  }

  def predict(
      projectId: String,
      features: Features,
      entityLinks: List[EntityLink],
      optionalAlgorithmId: Option[String]
  ): IO[Either[PredictionError, Prediction]] =
    projectsService.readProject(projectId).flatMap {
      case Some(project) =>
        val maybeAlgorithmId = optionalAlgorithmId.fold(
          project.policy.take()
        )(algorithmId => algorithmId.some)

        maybeAlgorithmId
          .fold[IO[Either[PredictionError, Prediction]]] {
            val errorMessage =
              s"There is no algorithm in project $projectId, prediction failed"
            logger.warn(errorMessage) *> IO.pure(
              NoAlgorithmAvailableError().asLeft
            )
          } { algorithmId =>
            project.algorithmsMap
              .get(algorithmId)
              .fold[IO[Either[PredictionError, Prediction]]] {
                val errorMessage = s"The algorithm $algorithmId does not exist"
                logger.warn(errorMessage) >> IO
                  .pure(
                    AlgorithmDoesNotExistError(
                      AlgorithmDoesNotExistError.message(algorithmId)
                    ).asLeft
                  )
              }(
                algorithm =>
                  IO(ju.UUID.randomUUID.toString)
                    .flatMap { predictionId =>
                      backendService.predictWithBackend(
                        predictionId,
                        project,
                        algorithm,
                        features
                      )
                    }
                    .flatMap {
                      case Right(prediction) =>
                        if (config.prediction.storeInPostgresql) {
                          logger.debug(
                            s"storing prediction ${prediction.id} in postgresql"
                          ) *> persistPrediction(prediction, entityLinks) *> IO
                            .pure(
                              prediction.asRight
                            )
                        } else {
                          logger.debug(
                            s"storing predictions in postgresql in disabled, ignoring ${prediction.id}"
                          ) *> IO.pure(prediction.asRight)
                        }
                      case Left(err) =>
                        IO.pure(err.asLeft)
                    }
              )
          }
      case None =>
        IO.pure(
          ProjectDoesNotExistError(
            ProjectDoesNotExistError.message(projectId)
          ).asLeft
        )
    }

  def addClassificationExample(
      labels: Option[String],
      predictionId: String,
      prediction: ClassificationPrediction
  ): ConnectionIO[Either[PredictionError, PredictionEvent]] =
    labels.fold[ConnectionIO[Either[PredictionError, PredictionEvent]]](
      (ClassificationExampleCannotBeEmptyError(): PredictionError).asLeft
        .pure[ConnectionIO]
    )(
      label =>
        prediction.labels
          .find(_.label == label)
          .fold[ConnectionIO[Either[PredictionError, PredictionEvent]]](
            (ClassificationExampleDoesNotExist(
              ClassificationExampleDoesNotExist.message(
                label,
                prediction.labels
              )
            ): PredictionError).asLeft.pure[ConnectionIO]
          )(
            _ => {
              val example = label
              val examples = prediction.examples :+ example
              val predictionEvent: PredictionEvent =
                ClassificationPredictionEvent(
                  ju.UUID.randomUUID().toString,
                  predictionId,
                  prediction.projectId,
                  prediction.algorithmId,
                  prediction.features,
                  prediction.labels,
                  example
                )

              predictionsRepository
                .updateClassificationExamples(predictionId, examples)
                .map(_ => predictionEvent.asRight)
            }
          )
    )

  def addRegressionExample(
      valueOpt: Option[Float],
      predictionId: String,
      prediction: RegressionPrediction
  ): ConnectionIO[Either[PredictionError, PredictionEvent]] =
    valueOpt.fold[ConnectionIO[Either[PredictionError, PredictionEvent]]](
      AsyncConnectionIO.pure(
        RegressionExampleShouldBeFloatError().asLeft
      )
    )(
      value => {
        val example = value
        val examples = prediction.examples :+ example

        val predictionEvent = RegressionPredictionEvent(
          ju.UUID.randomUUID().toString,
          predictionId,
          prediction.projectId,
          prediction.algorithmId,
          prediction.features,
          prediction.labels,
          example
        )
        predictionsRepository
          .updateRegressionExamples(predictionId, examples)
          .map(_ => predictionEvent.asRight)
      }
    )

  def addExample(
      predictionId: String,
      labelOpt: Option[String],
      valueOpt: Option[Float]
  ): IO[Either[PredictionError, PredictionEvent]] = {

    val transaction
        : ConnectionIO[Either[PredictionError, (Project, PredictionEvent)]] =
      predictionsRepository.read(predictionId).flatMap {

        case Some(prediction) =>
          AsyncConnectionIO
            .liftIO(
              Effect[IO].toIO(
                projectsService.readProject(prediction.projectId)
              )
            )
            .flatMap {
              case Some(project) =>
                val exampleIO
                    : ConnectionIO[Either[PredictionError, PredictionEvent]] =
                  prediction match {
                    case prediction: ClassificationPrediction =>
                      addClassificationExample(
                        labelOpt,
                        predictionId,
                        prediction
                      )
                    case prediction: RegressionPrediction =>
                      addRegressionExample(valueOpt, predictionId, prediction)
                  }
                exampleIO.map {
                  case Right(event) => (project, event).asRight[PredictionError]
                  case Left(err) => err.asLeft[(Project, PredictionEvent)]
                }
              case None =>
                (ProjectDoesNotExistError(
                  ProjectDoesNotExistError.message(prediction.projectId)
                ): PredictionError).asLeft.pure[ConnectionIO]
            }
        case None =>
          (PredictionDoesNotExistError(
            PredictionDoesNotExistError.message(predictionId)
          ): PredictionError).asLeft.pure[ConnectionIO]
      }
    predictionsRepository.transact(transaction).flatMap {
      case Right(projectEvent) =>
        val (project, event) = projectEvent
        logger.info("gonna publish") *> publishToStream(
          event,
          project.configuration.dataStream
        ).map(_ => event.asRight)
      case Left(err) =>
        IO.pure(err.asLeft)
    }
  }

}
