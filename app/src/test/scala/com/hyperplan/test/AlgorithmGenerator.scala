package com.hyperplan.test

import java.util.UUID

import com.hyperplan.domain.models.backends.LocalClassification
import com.hyperplan.domain.models._

object AlgorithmGenerator {

  val computed =
    Set(
      "class1",
      "class2",
      "class3"
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
