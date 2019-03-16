package com.foundaml.server.test

import java.util.UUID

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.features.CustomFeatures
import com.foundaml.server.domain.models.labels.Labels

object ProjectGenerator {

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

  val projectId = UUID.randomUUID().toString
  val defaultAlgorithmId = "algorithm id"

  def withLocalBackend() = Project(
    projectId,
    "example project",
    ProjectConfiguration(
      Classification(),
      CustomFeatures.featuresClass,
      10,
      "tf.cl",
      5
    ),
    Nil,
    DefaultAlgorithm(defaultAlgorithmId)
  )
}
