package com.foundaml.server.domain.models.labels

case class Labels(labels: Set[Label])

sealed trait Label {
  def id: String
}

case class ClassificationLabel(
    id: String,
    label: String,
    probability: Float,
    correctExampleUrl: String,
    incorrectExampleUrl: String
) extends Label

case class RegressionLabel(id: String, label: Float, correctExampleUrl: String)
    extends Label
