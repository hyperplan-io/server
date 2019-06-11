package com.foundaml.server.domain.repositories

import com.foundaml.server.domain.models.Examples.{
  ClassificationExamples,
  RegressionExamples
}
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.labels.{
  ClassificationLabel,
  Label,
  Labels,
  RegressionLabel
}
import com.foundaml.server.domain.repositories.PredictionsRepository.PredictionData
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.serialization.examples.{
  ClassificationExamplesSerializer,
  RegressionExamplesSerializer
}
import com.foundaml.server.infrastructure.serialization.features.FeaturesSerializer
import com.foundaml.server.infrastructure.serialization.labels.{
  ClassificationLabelSerializer,
  RegressionLabelSerializer
}
import doobie._
import doobie.implicits._
import doobie.postgres.sqlstate
import io.circe.{Decoder, Encoder}
import cats.effect.IO
import cats.implicits._

class PredictionsRepository(implicit xa: Transactor[IO]) extends IOLogging {

  implicit val featuresGet: Get[Either[io.circe.Error, Features]] =
    Get[String].map(FeaturesSerializer.decodeJson)
  implicit val featuresPut: Put[Features] =
    Put[String].contramap(FeaturesSerializer.encodeJsonNoSpaces)

  implicit val problemTypeGet: Get[Either[io.circe.Error, ProblemType]] =
    Get[String].map(ProblemTypeSerializer.decodeJson)
  implicit val problemTypePut: Put[ProblemType] =
    Put[String].contramap(ProblemTypeSerializer.encodeJsonString)

  implicit val labelsGet: Get[Either[io.circe.Error, Set[Label]]] =
    Get[String].map(LabelSerializer.decodeJsonSet)
  implicit val labelsPut: Put[Set[Label]] =
    Put[String].contramap(LabelSerializer.encodeJsonSetNoSpaces)

  implicit val classificationLabelsGet
      : Get[Either[io.circe.Error, Set[ClassificationLabel]]] =
    Get[String].map(ClassificationLabelSerializer.decodeJsonSet)
  implicit val classificationLabelsPut: Put[Set[ClassificationLabel]] =
    Put[String].contramap(ClassificationLabelSerializer.encodeJsonSetNoSpaces)

  implicit val regressionLabelsGet
      : Get[Either[io.circe.Error, Set[RegressionLabel]]] =
    Get[String].map(RegressionLabelSerializer.decodeJsonSet)
  implicit val regressionLabelsPut: Put[Set[RegressionLabel]] =
    Put[String].contramap(RegressionLabelSerializer.encodeJsonSetNoSpaces)

  implicit val classificationExamplesGet
      : Get[Either[io.circe.Error, ClassificationExamples]] =
    Get[String].map(ClassificationExamplesSerializer.decodeJson)
  implicit val classificationExamplesPut: Put[ClassificationExamples] =
    Put[String].contramap(ClassificationExamplesSerializer.encodeJsonNoSpaces)

  implicit val regressionExamplesGet
      : Get[Either[io.circe.Error, RegressionExamples]] =
    Get[String].map(RegressionExamplesSerializer.decodeJson)
  implicit val regressionExamplesPut: Put[RegressionExamples] =
    Put[String].contramap(RegressionExamplesSerializer.encodeJsonNoSpaces)

  def insertClassificationPredictionQuery(
      prediction: ClassificationPrediction
  ): doobie.Update0 =
    sql"""INSERT INTO predictions(
      id,
      project_id,
      algorithm_id,
      type,
      features,
      labels,
      examples
    ) VALUES(
      ${prediction.id},
      ${prediction.projectId},
      ${prediction.algorithmId},
      ${prediction.predictionType},
      ${prediction.features},
      ${prediction.labels},
      ${prediction.examples}
    )""".update

  def insertClassificationPrediction(
      prediction: ClassificationPrediction
  ): IO[Either[PredictionError, Int]] =
    insertClassificationPredictionQuery(prediction).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          PredictionAlreadyExist(prediction.id)
      }
      .transact(xa)

  def insertRegressionPredictionQuery(
      prediction: RegressionPrediction
  ): doobie.Update0 =
    sql"""INSERT INTO predictions(
      id,
      project_id,
      algorithm_id,
      type,
      features,
      labels,
      examples
    ) VALUES(
      ${prediction.id},
      ${prediction.projectId},
      ${prediction.algorithmId},
      ${prediction.predictionType},
      ${prediction.features},
      ${prediction.labels},
      ${prediction.examples}
    )""".update

  def insertRegressionPrediction(
      prediction: RegressionPrediction
  ): IO[Either[PredictionError, Int]] =
    insertRegressionPredictionQuery(prediction).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          PredictionAlreadyExist(prediction.id)
      }
      .transact(xa)

  def readQuery(predictionId: String) =
    sql"""
      SELECT id, project_id, algorithm_id, type, features, labels, examples
      FROM predictions
      WHERE id=$predictionId
      """
      .query[PredictionData]

  def read(predictionId: String): ConnectionIO[Prediction] =
    readQuery(predictionId).unique.flatMap(predictionFromData)

  def updateClassificationExamplesQuery(
      predictionId: String,
      examples: ClassificationExamples
  ) =
    sql"""UPDATE predictions SET examples = $examples WHERE id=$predictionId""".update

  def updateClassificationExamples(
      predictionId: String,
      examples: ClassificationExamples
  ) =
    updateClassificationExamplesQuery(predictionId, examples).run

  def updateRegressionExamplesQuery(
      predictionId: String,
      examples: RegressionExamples
  ) =
    sql"""UPDATE predictions SET examples = $examples WHERE id=$predictionId""".update

    def deletePredictionsLinkedToEntity(
      entityName: String,
      entityId: String
  ) = deletePredictionsLinkedToEntityQuery(entityName, entityId).run

  def deletePredictionsLinkedToEntityQuery(
      entityName: String,
      entityId: String
  ) =
    sql"""DELETE FROM predictions WHERE entity @> '{"$entityName": "$entityId"}'""".update


  def updateRegressionExamples(
      predictionId: String,
      examples: RegressionExamples
  ) =
    updateRegressionExamplesQuery(predictionId, examples).run

  def transact[T](connectionIO: ConnectionIO[T]): IO[T] =
    connectionIO.transact(xa)

  def predictionFromData(
      predictionData: PredictionData
  ): ConnectionIO[Prediction] =
    predictionData match {
      case (
          predictionId,
          projectId,
          algorithmId,
          Right(Classification),
          Right(features),
          labelsRaw,
          examplesRaw
          ) =>
        ClassificationLabelSerializer
          .decodeJsonSet(labelsRaw)
          .fold(
            err =>
              AsyncConnectionIO
                .raiseError(CouldNotDecodeLabels(predictionId, Classification)),
            classificationLabels => {
              ClassificationExamplesSerializer
                .decodeJson(examplesRaw)
                .fold(
                  err =>
                    AsyncConnectionIO.raiseError(
                      CouldNotDecodeExamples(predictionId, Classification)
                    ),
                  classificationExamples => {
                    val prediction = ClassificationPrediction(
                      predictionId,
                      projectId,
                      algorithmId,
                      features,
                      classificationExamples,
                      classificationLabels
                    )
                    AsyncConnectionIO.pure(prediction)
                  }
                )
            }
          )

      case (
          predictionId,
          projectId,
          algorithmId,
          Right(Regression),
          Right(features),
          labels,
          examplesRaw
          ) =>
        RegressionLabelSerializer
          .decodeJsonSet(labels)
          .fold(
            err =>
              AsyncConnectionIO
                .raiseError(CouldNotDecodeLabels(predictionId, Regression)),
            classificationLabels => {
              RegressionExamplesSerializer
                .decodeJson(examplesRaw)
                .fold(
                  err =>
                    AsyncConnectionIO.raiseError(
                      CouldNotDecodeExamples(predictionId, Classification)
                    ),
                  regressionExamples => {
                    val prediction = RegressionPrediction(
                      predictionId,
                      projectId,
                      algorithmId,
                      features,
                      regressionExamples,
                      classificationLabels
                    )
                    AsyncConnectionIO.pure(prediction)
                  }
                )
            }
          )
      case _ =>
        AsyncConnectionIO.raiseError(
          PredictionDataInconsistent(
            s"The prediction ${predictionData._1} cannot be restored"
          )
        )
    }
}

object PredictionsRepository {
  type PredictionData = (
      String,
      String,
      String,
      Either[io.circe.Error, ProblemType],
      Either[io.circe.Error, Features],
      String,
      String
  )
}
