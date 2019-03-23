package com.foundaml.server.test

import java.util.UUID

import com.foundaml.server.domain.models.backends.Local
import com.foundaml.server.domain.models.labels.Labels
import com.foundaml.server.domain.models.{Algorithm, labels}

object AlgorithmGenerator {

  val computed = Labels(
    Set(
      labels.ClassificationLabel(
        UUID.randomUUID().toString,
        "class1",
        0.1f,
        "correct_url",
        "incorrect_url"
      ),
      labels.ClassificationLabel(
        UUID.randomUUID().toString,
        "class2",
        0.2f,
        "correct_url",
        "incorrect_url"
      ),
      labels.ClassificationLabel(
        UUID.randomUUID().toString,
        "class3",
        0.3f,
        "correct_url",
        "incorrect_url"
      )
    )
  )

  val defaultAlgorithm = Algorithm(
    "algorithm id",
    Local(computed),
    "test project id"
  )

  def withLocalBackend(algorithmId: Option[String] = None) =
    Algorithm(
      algorithmId.getOrElse(UUID.randomUUID().toString),
      Local(computed),
      UUID.randomUUID().toString
    )

}
