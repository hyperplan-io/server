package com.foundaml.server.test

import java.util.UUID

import com.foundaml.server.domain.models.backends.Local
import com.foundaml.server.domain.models.labels.Labels
import com.foundaml.server.domain.models.{Algorithm, labels}

object AlgorithmGenerator {

  val computed = Labels(
    Set(
      labels.ClassificationLabel(
        "class1",
        0.1f
      ),
      labels.ClassificationLabel(
        "class2",
        0.2f
      ),
      labels.ClassificationLabel(
        "class3",
        0.3f
      )
    )
  )

  val defaultAlgorithm = Algorithm(
    "algorithm id",
    Local(computed),
    "test project id"
  )

  def withLocalBackend() =
    Algorithm(
      UUID.randomUUID().toString,
      Local(computed),
      UUID.randomUUID().toString
    )

}
