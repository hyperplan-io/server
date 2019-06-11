package com.hyperplan.server.test.domain.repositories

import java.util.UUID

import com.hyperplan.domain.models.ClassificationPrediction
import com.hyperplan.domain.models.features.StringFeature
import com.hyperplan.domain.models.labels.ClassificationLabel
import com.hyperplan.domain.repositories.PredictionsRepository
import com.hyperplan.server.test.{TaskChecker, TestDatabase}
import org.scalatest._

import scala.util.Random

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
            Random.nextString(10),
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
