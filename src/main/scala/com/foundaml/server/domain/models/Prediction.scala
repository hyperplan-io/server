package com.foundaml.server.domain.models

import com.foundaml.server.domain.models.Examples.ClassificationExamples
import com.foundaml.server.domain.models.features.Features.Features
import com.foundaml.server.domain.models.labels.{ClassificationLabel, Labels, RegressionLabel}

sealed trait Prediction {
  def id: String
  def projectId: String
  def algorithmId: String
  def features: Features
  def predictionType: ProblemType
}


case class ClassificationPrediction(id: String,
                                    projectId: String,
                                    algorithmId: String,
                                    features: Features,
                                    examples: ClassificationExamples,
                                    labels: Set[ClassificationLabel]) extends Prediction {
  override def predictionType: ProblemType = Classification()
}
case class RegressionPrediction(
                                 id: String,
                                 projectId: String,
                                 algorithmId: String,
                                 features: Features,
                                 examples: ClassificationExamples,
                                 labels: Set[RegressionLabel]) extends Prediction {
  override def predictionType: ProblemType = Regression()
}