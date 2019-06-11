package com.hyperplan.test

import java.util.UUID

import com.hyperplan.domain.models
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.labels.Labels
import com.hyperplan.domain.models.features.One
import com.hyperplan.domain.models.features.StringFeatureType

object ProjectGenerator {

  val computed = Labels(
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
  )

  val projectId = UUID.randomUUID().toString
  val defaultAlgorithmId = "algorithm id"

  def withLocalBackend(algorithms: Option[List[Algorithm]] = None) =
    ClassificationProject(
      projectId,
      "example project",
      ClassificationConfiguration(
        models.FeaturesConfiguration(
          "id",
          List(
            FeatureConfiguration(
              "my feature",
              StringFeatureType,
              One,
              "this is a description of the features"
            )
          )
        ),
        LabelsConfiguration(
          "id",
          OneOfLabelsConfiguration(
            Set(
              "class1",
              "class2",
              "class3"
            ),
            "Either class1, class2 or class3"
          )
        )
      ),
      algorithms.getOrElse(List(AlgorithmGenerator.withLocalBackend())),
      DefaultAlgorithm(defaultAlgorithmId)
    )
}
