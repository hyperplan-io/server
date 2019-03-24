package com.foundaml.server.domain.repositories

import com.foundaml.server.domain.models.Examples.ClassificationExamples
import com.foundaml.server.domain.models.errors.{PredictionAlreadyExist, PredictionError}
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.labels.{ClassificationLabel, Label, Labels, RegressionLabel}
import com.foundaml.server.domain.repositories.PredictionsRepository.PredictionData
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.serialization.features.FeaturesSerializer
import com.foundaml.server.infrastructure.serialization.labels.{ClassificationLabelSerializer, RegressionLabelSerializer}
import doobie._
import doobie.implicits._
import doobie.postgres.sqlstate
import io.circe.{Decoder, Encoder}
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class PredictionsRepository(implicit xa: Transactor[Task]) {

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

  implicit val classificationLabelsGet: Get[Either[io.circe.Error, Set[ClassificationLabel]]] =
    Get[String].map(ClassificationLabelSerializer.decodeJsonSet)
  implicit val classificationLabelsPut: Put[Set[ClassificationLabel]] =
    Put[String].contramap(ClassificationLabelSerializer.encodeJsonSetNoSpaces)

  implicit val regressionLabelsGet: Get[Either[io.circe.Error, Set[RegressionLabel]]] =
    Get[String].map(RegressionLabelSerializer.decodeJsonSet)
  implicit val regressionLabelsPut: Put[Set[RegressionLabel]] =
    Put[String].contramap(RegressionLabelSerializer.encodeJsonSetNoSpaces)

  implicit val examplesGet: Get[Either[io.circe.Error, ClassificationExamples]] =
    Get[String].map(ExamplesSerializer.decodeJson)
  implicit val examplesPut: Put[ClassificationExamples] =
    Put[String].contramap(ExamplesSerializer.encodeJsonNoSpaces)

  def insertClassificationPredictionQuery(prediction: ClassificationPrediction): doobie.Update0 =
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

  def insertClassificationPrediction(prediction: ClassificationPrediction): Task[Either[PredictionError, Int]] =
    insertClassificationPredictionQuery(prediction).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          PredictionAlreadyExist(prediction.id)
      }
      .transact(xa)

  def insertRegressionPredictionQuery(prediction: RegressionPrediction): doobie.Update0 =
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

  def insertRegressionPrediction(prediction: RegressionPrediction): Task[Either[PredictionError, Int]] =
    insertRegressionPredictionQuery(prediction).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          PredictionAlreadyExist(prediction.id)
      }
      .transact(xa)

  def readQuery(predictionId: String) =
    sql"""
      SELECT id, project_id, algorithm_id, prediction_type, features, labels, examples
      FROM predictions
      WHERE id=$predictionId
      """
      .query[PredictionData]

  def read(predictionId: String) =
    readQuery(predictionId).unique.transact(xa)

  def updateExamplesQuery(predictionId: String, examples: ClassificationExamples) =
    sql"""UPDATE predictions SET examples = $examples WHERE id=$predictionId""".update

  def updateExamples(predictionId: String, examples: ClassificationExamples) =
    updateExamplesQuery(predictionId, examples).run.transact(xa)
}

object PredictionsRepository {
  type PredictionData = (String, String, String, Either[io.circe.Error, ProblemType], Either[io.circe.Error, Features], Either[io.circe.Error, Set[Label]], Either[io.circe.Error, ClassificationExamples])
}