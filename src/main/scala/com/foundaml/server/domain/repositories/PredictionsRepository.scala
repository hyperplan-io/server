package com.foundaml.server.domain.repositories

import com.foundaml.server.domain.models.{Examples, Prediction}
import com.foundaml.server.domain.models.features.Features
import com.foundaml.server.domain.models.labels.Labels
import com.foundaml.server.infrastructure.serialization._
import doobie._
import doobie.implicits._
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class PredictionsRepository(implicit xa: Transactor[Task]) {

  implicit val featuresGet: Get[Features] =
    Get[String].map(FeaturesSerializer.decodeJson)
  implicit val featuresPut: Put[Features] =
    Put[String].contramap(FeaturesSerializer.encodeJsonNoSpaces)

  implicit val labelsGet: Get[Labels] =
    Get[String].map(LabelsSerializer.decodeJson)
  implicit val labelsPut: Put[Labels] =
    Put[String].contramap(LabelsSerializer.encodeJsonNoSpaces)

  implicit val examplesGet: Get[Examples] =
    Get[String].map(ExamplesSerializer.decodeJson)
  implicit val examplesPut: Put[Examples] =
    Put[String].contramap(ExamplesSerializer.encodeJsonNoSpaces)

  def insertQuery(prediction: Prediction): doobie.Update0 =
    sql"""INSERT INTO predictions(
      id,
      project_id,
      algorithm_id,
      features,
      labels,
      examples
    ) VALUES(
      ${prediction.id},
      ${prediction.projectId},
      ${prediction.algorithmId},
      ${prediction.features},
      ${prediction.labels},
      ${prediction.examples}
    )""".update

  def insert(prediction: Prediction): Task[Int] =
    insertQuery(prediction).run.transact(xa)

  def readQuery(predictionId: String) =
    sql"""
      SELECT id, project_id, algorithm_id, features, labels, examples
      FROM predictions
      WHERE id=$predictionId
      """
      .query[Prediction]

  def read(predictionId: String) =
    readQuery(predictionId).unique.transact(xa)

  def updateExamplesQuery(predictionId: String, examples: Examples) =
    sql"""UPDATE predictions SET examples = $examples WHERE id=$predictionId""".update

  def updateExamples(predictionId: String, examples: Examples) =
    updateExamplesQuery(predictionId, examples).run.transact(xa)
}
