package com.foundaml.server.test.utils

import com.foundaml.server.domain.models.{labels, _}
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.models.backends._
import java.util.UUID

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
