package com.hyperplan.test

import java.util.UUID

import com.hyperplan.domain.models
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.labels.Labels
import com.hyperplan.domain.models.features.Scalar
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
        models.FeatureVectorDescriptor(
          "id",
          List(
            FeatureDescriptor(
              "my feature",
              StringFeatureType,
              Scalar,
              "this is a description of the features"
            )
          )
        ),
        LabelVectorDescriptor(
          "id",
          OneOfLabelsDescriptor(
            Set(
              "class1",
              "class2",
              "class3"
            ),
            "Either class1, class2 or class3"
          )
        ),
        None
      ),
      algorithms.getOrElse(List(AlgorithmGenerator.withLocalBackend())),
      DefaultAlgorithm(defaultAlgorithmId)
    )
}
