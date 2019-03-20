package com.foundaml.server.test

import java.util.UUID

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.features.CustomFeatures
import com.foundaml.server.domain.models.labels.Labels

object ProjectGenerator {

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

  val projectId = UUID.randomUUID().toString
  val defaultAlgorithmId = "algorithm id"

  def withLocalBackend() = Project(
    projectId,
    "example project",
    ProjectConfiguration(
      Classification(),
      StandardFeaturesConfiguration(
        CustomFeatures.featuresClass,
        10
      ),
      Set(
        "class1",
        "class2",
        "class3"
      )
    ),
    Nil,
    DefaultAlgorithm(defaultAlgorithmId)
  )
}
