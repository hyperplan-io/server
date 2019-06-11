package com.hyperplan.test

import java.util.UUID

import com.hyperplan.domain.models.backends.LocalClassification
import com.hyperplan.domain.models._

object AlgorithmGenerator {

  val computed =
    Set(
      labels.ClassificationLabel(
        "class1",
        0.1f,
        "correct_url",
        "incorrect_url"
      ),
      labels.ClassificationLabel(
        "class2",
        0.2f,
        "correct_url",
        "incorrect_url"
      ),
      labels.ClassificationLabel(
        "class3",
        0.3f,
        "correct_url",
        "incorrect_url"
      )
    )

  val defaultSecurityConfig = PlainSecurityConfiguration(
    Nil
  )

  val defaultAlgorithm = Algorithm(
    "algorithm id",
    LocalClassification(computed),
    "test project id",
    defaultSecurityConfig
  )

  def withLocalBackend(algorithmId: Option[String] = None) =
    Algorithm(
      algorithmId.getOrElse(UUID.randomUUID().toString),
      LocalClassification(computed),
      UUID.randomUUID().toString,
      defaultSecurityConfig
    )

}
