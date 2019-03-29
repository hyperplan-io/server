package com.foundaml.server.test.domain.repositories

import java.util.UUID

import com.foundaml.server.domain.models.ClassificationPrediction
import com.foundaml.server.domain.models.features.StringFeature
import com.foundaml.server.domain.models.labels.ClassificationLabel
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  PredictionsRepository
}
import com.foundaml.server.test.{AlgorithmGenerator, TaskChecker, TestDatabase}
import org.scalatest._

class PredictionsRepositorySpec
    extends FlatSpec
    with Matchers
    with TaskChecker
    with TestDatabase {

  val predictionsRepository = new PredictionsRepository()(xa)

  it should "insert and read predictions correctly" in {

    withInMemoryDatabase { _ =>
      val query = ClassificationPrediction(
        UUID.randomUUID().toString,
        UUID.randomUUID().toString,
        UUID.randomUUID().toString,
        List(
          StringFeature(
            "x"
          )
        ),
        List.empty,
        Set(
          ClassificationLabel(
            "label1",
            0.0f,
            "correct url",
            "incorrect url"
          )
        )
      )
      val insertIO =
        predictionsRepository.insertClassificationPredictionQuery(query)
      val readIO = predictionsRepository.readQuery(query.id)
      check(insertIO)
      check(readIO)
    }
  }

}
